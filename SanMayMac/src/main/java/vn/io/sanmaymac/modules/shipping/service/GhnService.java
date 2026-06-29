package vn.io.sanmaymac.modules.shipping.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.io.sanmaymac.modules.order.entity.OrderDetailEntity;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.shipping.dto.DistrictRecord;
import vn.io.sanmaymac.modules.shipping.dto.ProvinceRecord;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteResponseRecord;
import vn.io.sanmaymac.modules.shipping.dto.WardRecord;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;

@Service
public class GhnService {
	private static final Logger log = LoggerFactory.getLogger(GhnService.class);
	private static final String DEFAULT_BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api";
	private static final String DEV_BASE_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api";

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.ghn.token:}")
	private String ghnToken;

	@Value("${app.ghn.shop-id:}")
	private String ghnShopId;

	@Value("${app.ghn.from-district-id:0}")
	private int fromDistrictId;

	@Value("${app.ghn.from-ward-code:}")
	private String fromWardCode;

	@Value("${app.ghn.service-type-id:2}")
	private int serviceTypeId;

	@Value("${app.ghn.base-url:" + DEFAULT_BASE_URL + "}")
	private String baseUrl;

	private String activeBaseUrl;
	private volatile boolean pickupResolved;
	private volatile String lastError;

	@PostConstruct
	void initGhnConfig() {
		ghnToken = trim(ghnToken);
		ghnShopId = trim(ghnShopId);
		fromWardCode = trim(fromWardCode);
		activeBaseUrl = trim(baseUrl);
		if (activeBaseUrl.isBlank()) {
			activeBaseUrl = DEV_BASE_URL;
		}
		log.info("GHN gateway: {} (shopId={})", activeBaseUrl, ghnShopId.isBlank() ? "none" : ghnShopId);
	}

	public String getLastError() {
		return lastError;
	}

	public boolean hasToken() {
		return ghnToken != null && !ghnToken.isBlank();
	}

	public boolean hasShippingConfig() {
		return hasToken() && ghnShopId != null && !ghnShopId.isBlank();
	}

	public List<ProvinceRecord> listProvinces() {
		lastError = null;
		if (!hasToken()) {
			lastError = "Chưa cấu hình GHN_TOKEN trong .env";
			return Collections.emptyList();
		}
		try {
			Map<String, Object> body = request("GET", "/master-data/province", null, false);
			if (!isSuccess(body)) {
				lastError = formatGhnError(body);
				log.warn("GHN provinces failed: {}", lastError);
				return Collections.emptyList();
			}
			List<Map<String, Object>> data = readDataList(body);
			List<ProvinceRecord> result = new ArrayList<>();
			for (Map<String, Object> item : data) {
				result.add(new ProvinceRecord(
						toInt(firstValue(item, "ProvinceID", "province_id")),
						stringValue(firstValue(item, "ProvinceName", "province_name"))));
			}
			return result;
		} catch (Exception ex) {
			lastError = ex.getMessage();
			log.warn("GHN provinces error: {}", ex.getMessage());
			return Collections.emptyList();
		}
	}

	public List<DistrictRecord> listDistricts(int provinceId) {
		if (!hasToken()) {
			return Collections.emptyList();
		}
		try {
			Map<String, Object> payload = Map.of("province_id", provinceId);
			Map<String, Object> body = request("POST", "/master-data/district", payload, false);
			if (!isSuccess(body)) {
				log.warn("GHN districts failed: {}", body.get("message"));
				return Collections.emptyList();
			}
			List<Map<String, Object>> data = readDataList(body);
			List<DistrictRecord> result = new ArrayList<>();
			for (Map<String, Object> item : data) {
				result.add(new DistrictRecord(
						toInt(firstValue(item, "DistrictID", "district_id")),
						stringValue(firstValue(item, "DistrictName", "district_name"))));
			}
			return result;
		} catch (Exception ex) {
			log.warn("GHN districts error: {}", ex.getMessage());
			return Collections.emptyList();
		}
	}

	public List<WardRecord> listWards(int districtId) {
		if (!hasToken()) {
			return Collections.emptyList();
		}
		try {
			Map<String, Object> payload = Map.of("district_id", districtId);
			Map<String, Object> body = request("POST", "/master-data/ward", payload, false);
			if (!isSuccess(body)) {
				log.warn("GHN wards failed: {}", body.get("message"));
				return Collections.emptyList();
			}
			List<Map<String, Object>> data = readDataList(body);
			List<WardRecord> result = new ArrayList<>();
			for (Map<String, Object> item : data) {
				result.add(new WardRecord(
						stringValue(firstValue(item, "WardCode", "ward_code")),
						stringValue(firstValue(item, "WardName", "ward_name"))));
			}
			return result;
		} catch (Exception ex) {
			log.warn("GHN wards error: {}", ex.getMessage());
			return Collections.emptyList();
		}
	}

	public ShippingQuoteResponseRecord calculateFee(UserAddressEntity address, int totalQuantity) {
		if (!hasShippingConfig()) {
			return new ShippingQuoteResponseRecord(
					BigDecimal.ZERO,
					null,
					false,
					"GHN chưa được cấu hình (cần GHN_TOKEN và GHN_SHOP_ID).");
		}
		if (address.getDistrictId() == null || address.getWardCode() == null || address.getWardCode().isBlank()) {
			return new ShippingQuoteResponseRecord(
					BigDecimal.ZERO,
					null,
					false,
					"Địa chỉ thiếu thông tin quận/huyện hoặc phường/xã.");
		}

		resolvePickupFromShopIfNeeded();

		int weight = Math.max(200, totalQuantity * 500);
		Map<String, Object> payload = new HashMap<>();
		payload.put("service_type_id", serviceTypeId);
		payload.put("to_district_id", address.getDistrictId());
		payload.put("to_ward_code", address.getWardCode());
		payload.put("height", 10);
		payload.put("length", 30);
		payload.put("width", 20);
		payload.put("weight", weight);
		payload.put("insurance_value", 0);
		applyPickupAddress(payload);

		try {
			Map<String, Object> body = request("POST", "/v2/shipping-order/fee", payload, true);
			if (!isSuccess(body)) {
				return new ShippingQuoteResponseRecord(
						BigDecimal.ZERO,
						null,
						false,
						stringValue(body.get("message")));
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> data = (Map<String, Object>) body.get("data");
			BigDecimal total = toBigDecimal(data.get("total"));
			return new ShippingQuoteResponseRecord(total, "GHN", true, "OK");
		} catch (Exception ex) {
			log.warn("GHN fee error: {}", ex.getMessage());
			return new ShippingQuoteResponseRecord(
					BigDecimal.ZERO,
					null,
					false,
					"Không thể tính phí GHN: " + ex.getMessage());
		}
	}

	public String createShippingOrder(OrderEntity order, List<OrderDetailEntity> details) {
		if (!hasShippingConfig()) {
			return null;
		}
		UserAddressEntity address = order.getAddress();
		if (address == null || address.getDistrictId() == null || address.getWardCode() == null) {
			throw new IllegalStateException("Order address is incomplete for GHN");
		}

		resolvePickupFromShopIfNeeded();

		int totalQuantity = details.stream()
				.mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 1)
				.sum();
		int weight = Math.max(200, totalQuantity * 500);

		List<Map<String, Object>> items = new ArrayList<>();
		for (OrderDetailEntity detail : details) {
			Map<String, Object> item = new HashMap<>();
			item.put("name", detail.getProductName() != null ? detail.getProductName() : "Sản phẩm");
			item.put("quantity", detail.getQuantity() != null ? detail.getQuantity() : 1);
			item.put("weight", 500);
			items.add(item);
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("payment_type_id", 2);
		payload.put("note", order.getCustomerNote() != null ? order.getCustomerNote() : "");
		payload.put("required_note", "KHONGCHOXEMHANG");
		payload.put("to_name", address.getReceiverName());
		payload.put("to_phone", address.getPhone());
		payload.put("to_address", address.getDetailedAddress());
		payload.put("to_ward_code", address.getWardCode());
		payload.put("to_district_id", address.getDistrictId());
		payload.put("weight", weight);
		payload.put("length", 30);
		payload.put("width", 20);
		payload.put("height", 10);
		payload.put("service_type_id", serviceTypeId);
		payload.put("insurance_value", order.getTotalAmount() != null ? order.getTotalAmount().intValue() : 0);
		payload.put("items", items);
		payload.put("client_order_code", "SM-" + order.getId());
		applyPickupAddress(payload);

		Map<String, Object> body = request("POST", "/v2/shipping-order/create", payload, true);
		if (!isSuccess(body)) {
			throw new IllegalStateException("GHN create order failed: " + stringValue(body.get("message")));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) body.get("data");
		String orderCode = stringValue(data.get("order_code"));
		if (orderCode == null || orderCode.isBlank()) {
			throw new IllegalStateException("GHN did not return order_code");
		}
		return orderCode;
	}

	private void applyPickupAddress(Map<String, Object> payload) {
		if (fromDistrictId > 0 && fromWardCode != null && !fromWardCode.isBlank()) {
			payload.put("from_district_id", fromDistrictId);
			payload.put("from_ward_code", fromWardCode);
		}
	}

	private void resolvePickupFromShopIfNeeded() {
		if (pickupResolved) {
			return;
		}
		synchronized (this) {
			if (pickupResolved) {
				return;
			}
			if (fromDistrictId > 0 && fromWardCode != null && !fromWardCode.isBlank()) {
				pickupResolved = true;
				return;
			}
			try {
				Map<String, Object> body = request("GET", "/v2/shop/all", null, false);
				if (!isSuccess(body)) {
					log.warn("GHN shop lookup failed: {}", body.get("message"));
					pickupResolved = true;
					return;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> data = (Map<String, Object>) body.get("data");
				if (data == null) {
					pickupResolved = true;
					return;
				}
				Object shopsObj = data.get("shops");
				if (!(shopsObj instanceof List<?> shops)) {
					pickupResolved = true;
					return;
				}
				int targetShopId = Integer.parseInt(ghnShopId.trim());
				for (Object shopObj : shops) {
					if (!(shopObj instanceof Map<?, ?> shopMapRaw)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> shop = (Map<String, Object>) shopMapRaw;
					Integer shopId = toInt(firstValue(shop, "_id", "shop_id", "id"));
					if (shopId == null || shopId != targetShopId) {
						continue;
					}
					Integer districtId = toInt(firstValue(shop, "district_id", "DistrictID"));
					String wardCode = stringValue(firstValue(shop, "ward_code", "WardCode"));
					if (districtId != null && wardCode != null && !wardCode.isBlank()) {
						fromDistrictId = districtId;
						fromWardCode = wardCode;
						log.info("GHN pickup resolved from shop {}: district={}, ward={}",
								targetShopId, fromDistrictId, fromWardCode);
					}
					break;
				}
			} catch (Exception ex) {
				log.warn("GHN shop lookup error: {}", ex.getMessage());
			} finally {
				pickupResolved = true;
			}
		}
	}

	private Map<String, Object> request(String method, String path, Map<String, Object> payload, boolean includeShopId) {
		Map<String, Object> body = executeRequest(activeBaseUrl, method, path, payload, includeShopId);
		if (isTokenInvalid(body) && !activeBaseUrl.contains("dev-online-gateway")) {
			log.warn("GHN token rejected on {}, retrying dev gateway", activeBaseUrl);
			Map<String, Object> devBody = executeRequest(DEV_BASE_URL, method, path, payload, includeShopId);
			if (isSuccess(devBody)) {
				activeBaseUrl = DEV_BASE_URL;
				log.info("GHN switched to dev gateway: {}", DEV_BASE_URL);
				return devBody;
			}
			return devBody;
		}
		return body;
	}

	private Map<String, Object> executeRequest(
			String url,
			String method,
			String path,
			Map<String, Object> payload,
			boolean includeShopId) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder()
					.uri(URI.create(url + path))
					.header("Content-Type", "application/json")
					.header("token", ghnToken)
					.header("Token", ghnToken);
			if (includeShopId && !ghnShopId.isBlank()) {
				builder.header("ShopId", ghnShopId);
			}
			HttpRequest httpRequest;
			if ("GET".equalsIgnoreCase(method) || payload == null) {
				httpRequest = builder.GET().build();
			} else {
				httpRequest = builder
						.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
						.build();
			}
			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception ex) {
			throw new IllegalStateException("GHN request failed: " + ex.getMessage(), ex);
		}
	}

	private boolean isTokenInvalid(Map<String, Object> body) {
		if (body == null) {
			return true;
		}
		Object code = body.get("code");
		if (code instanceof Number number && number.intValue() == 401) {
			return true;
		}
		String message = stringValue(body.get("message"));
		return message != null && message.toLowerCase().contains("token");
	}

	private String formatGhnError(Map<String, Object> body) {
		String message = stringValue(body.get("message"));
		if (isTokenInvalid(body)) {
			return "Token GHN không hợp lệ. Lấy token mới tại https://api.ghn.vn → Token API. "
					+ "Token Production dùng GHN_BASE_URL production; token Sandbox dùng: "
					+ DEV_BASE_URL;
		}
		return message != null ? message : "GHN request failed";
	}

	private String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean isSuccess(Map<String, Object> body) {
		Object code = body.get("code");
		if (code instanceof Number number) {
			return number.intValue() == 200;
		}
		return "200".equals(String.valueOf(code));
	}

	private List<Map<String, Object>> readDataList(Map<String, Object> body) {
		Object data = body.get("data");
		if (!(data instanceof List<?> list)) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cast = (Map<String, Object>) map;
				result.add(cast);
			}
		}
		return result;
	}

	private Object firstValue(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			if (map.containsKey(key) && map.get(key) != null) {
				return map.get(key);
			}
		}
		return null;
	}

	private Integer toInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return null;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		if (value == null) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
