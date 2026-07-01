package vn.io.sanmaymac.modules.bidding.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.io.sanmaymac.common.enums.PostStatus;
import vn.io.sanmaymac.common.enums.QuoteStatus;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.bidding.dto.AcceptQuoteRequest;
import vn.io.sanmaymac.modules.bidding.dto.BiddingDesignRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingDesignUploadResponseRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingAttachmentRequestRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingAttachmentResponseRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostCreateRequest;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostDetailRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostSummaryRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostUpdateRequest;
import vn.io.sanmaymac.modules.bidding.dto.QuoteCreateRequest;
import vn.io.sanmaymac.modules.bidding.dto.QuoteResponseRecord;
import vn.io.sanmaymac.modules.bidding.dto.QuoteUpdateRequest;
import vn.io.sanmaymac.modules.bidding.dto.UpdatePostStatusRequest;
import vn.io.sanmaymac.modules.bidding.entity.BiddingAttachmentEntity;
import vn.io.sanmaymac.modules.bidding.entity.BiddingDesignEntity;
import vn.io.sanmaymac.modules.bidding.entity.BiddingPostEntity;
import vn.io.sanmaymac.modules.bidding.entity.QuoteEntity;
import vn.io.sanmaymac.modules.bidding.repository.BiddingAttachmentRepository;
import vn.io.sanmaymac.modules.bidding.repository.BiddingDesignRepository;
import vn.io.sanmaymac.modules.bidding.repository.BiddingPostRepository;
import vn.io.sanmaymac.modules.bidding.repository.QuoteRepository;
import vn.io.sanmaymac.modules.order.dto.CheckoutCustomRequest;
import vn.io.sanmaymac.modules.order.dto.OrderDetailResponseRecord;
import vn.io.sanmaymac.modules.order.service.OrderService;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class BiddingService {
	private static final String SORT_LATEST = "latest";
	private static final String SORT_NO_QUOTES = "no-quotes";

	private final BiddingPostRepository postRepository;
	private final BiddingAttachmentRepository attachmentRepository;
	private final BiddingDesignRepository designRepository;
	private final QuoteRepository quoteRepository;
	private final UserRepository userRepository;
	private final OrderService orderService;
	private final BiddingDesignStorageService biddingDesignStorageService;

	public BiddingService(
			BiddingPostRepository postRepository,
			BiddingAttachmentRepository attachmentRepository,
			BiddingDesignRepository designRepository,
			QuoteRepository quoteRepository,
			UserRepository userRepository,
			OrderService orderService,
			BiddingDesignStorageService biddingDesignStorageService) {
		this.postRepository = postRepository;
		this.attachmentRepository = attachmentRepository;
		this.designRepository = designRepository;
		this.quoteRepository = quoteRepository;
		this.userRepository = userRepository;
		this.orderService = orderService;
		this.biddingDesignStorageService = biddingDesignStorageService;
	}

	public BiddingDesignUploadResponseRecord uploadDesignFiles(MultipartFile frontDesign, MultipartFile backDesign) {
		String frontUrl = biddingDesignStorageService.store(frontDesign, "front-design");
		String backUrl = biddingDesignStorageService.store(backDesign, "back-design");
		return new BiddingDesignUploadResponseRecord(frontUrl, backUrl);
	}

	public BiddingDesignRecord saveDesign(MultipartFile frontDesign, MultipartFile backDesign, String name) {
		UserEntity customer = getCurrentUser();
		String frontUrl = biddingDesignStorageService.store(frontDesign, "front-design");
		String backUrl = biddingDesignStorageService.store(backDesign, "back-design");

		BiddingDesignEntity design = BiddingDesignEntity.builder()
				.customer(customer)
				.name(name == null || name.isBlank() ? "Thiết kế chưa đặt tên" : name.trim())
				.frontDesignUrl(frontUrl)
				.backDesignUrl(backUrl)
				.build();

		return toBiddingDesignRecord(designRepository.save(design));
	}

	public List<BiddingDesignRecord> listMyDesigns() {
		UserEntity customer = getCurrentUser();
		return designRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
				.stream()
				.map(this::toBiddingDesignRecord)
				.toList();
	}

	public BiddingPostDetailRecord createPost(BiddingPostCreateRequest request) {
		UserEntity customer = getCurrentUser();
		BiddingPostEntity post = BiddingPostEntity.builder()
				.customer(customer)
				.title(request.title())
				.description(request.description())
				.aiImageUrl(request.aiImageUrl())
				.frontDesignUrl(request.frontDesignUrl())
				.backDesignUrl(request.backDesignUrl())
				.status(PostStatus.OPEN)
				.build();
		BiddingPostEntity saved = postRepository.save(post);
		saveAttachments(saved, request.attachments());
		return buildPostDetail(saved);
	}

	public Page<BiddingPostSummaryRecord> listMyPosts(Pageable pageable) {
		UserEntity customer = getCurrentUser();
		return postRepository.findByCustomerId(customer.getId(), pageable)
				.map(this::toPostSummary);
	}

	public BiddingPostDetailRecord updatePost(Long postId, BiddingPostUpdateRequest request) {
		BiddingPostEntity post = getPostForCustomer(postId);
		ensurePostEditable(post);
		post.setTitle(request.title());
		post.setDescription(request.description());
		post.setAiImageUrl(request.aiImageUrl());
		post.setFrontDesignUrl(request.frontDesignUrl());
		post.setBackDesignUrl(request.backDesignUrl());
		postRepository.save(post);

		attachmentRepository.deleteByPostId(post.getId());
		saveAttachments(post, request.attachments());
		return buildPostDetail(post);
	}

	public void deletePost(Long postId) {
		BiddingPostEntity post = getPostForCustomer(postId);
		ensurePostEditable(post);
		quoteRepository.deleteByPostId(post.getId());
		attachmentRepository.deleteByPostId(post.getId());
		postRepository.delete(post);
	}

	public BiddingPostDetailRecord getPostDetail(Long postId) {
		BiddingPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("Post not found"));
		return buildPostDetail(post);
	}

	public Page<QuoteResponseRecord> listPostQuotes(Long postId, Pageable pageable) {
		BiddingPostEntity post = getPostForCustomer(postId);
		return quoteRepository.findByPostId(post.getId(), pageable)
				.map(this::toQuoteResponse);
	}

	public OrderDetailResponseRecord acceptQuote(Long postId, Long quoteId, AcceptQuoteRequest request) {
		BiddingPostEntity post = getPostForCustomer(postId);
		if (!PostStatus.OPEN.equals(post.getStatus())) {
			throw new IllegalStateException("Post is not open");
		}

		QuoteEntity selected = quoteRepository.findById(quoteId)
				.orElseThrow(() -> new IllegalArgumentException("Quote not found"));
		if (selected.getPost() == null || !selected.getPost().getId().equals(post.getId())) {
			throw new IllegalStateException("Quote does not belong to post");
		}
		if (!QuoteStatus.PENDING.equals(selected.getStatus())) {
			throw new IllegalStateException("Quote is not pending");
		}

		post.setStatus(PostStatus.CLOSED);
		postRepository.save(post);

		List<QuoteEntity> quotes = quoteRepository.findByPostId(post.getId());
		for (QuoteEntity quote : quotes) {
			if (quote.getId().equals(selected.getId())) {
				quote.setStatus(QuoteStatus.ACCEPTED);
			} else {
				quote.setStatus(QuoteStatus.REJECTED);
			}
			quoteRepository.save(quote);
		}

		return orderService.checkoutCustom(new CheckoutCustomRequest(selected.getId(), request.addressId()));
	}

	    public Page<BiddingPostSummaryRecord> exploreOpenPosts(
				String keyword,
				String sort,
				Integer maxQuotes,
				Pageable pageable) {
		List<BiddingPostEntity> posts = postRepository.findByStatus(PostStatus.OPEN);
		List<BiddingPostSummaryRecord> summaries = posts.stream()
			.filter(post -> keyword == null || keyword.isBlank()
				|| containsIgnoreCase(post.getTitle(), keyword)
				|| containsIgnoreCase(post.getDescription(), keyword))
			.map(this::toPostSummary)
			.filter(summary -> maxQuotes == null || summary.quoteCount() <= maxQuotes)
			.toList();

		String normalizedSort = sort == null ? SORT_LATEST : sort.toLowerCase(Locale.ROOT);
		if (SORT_NO_QUOTES.equals(normalizedSort)) {
		    summaries = summaries.stream()
			    .sorted(Comparator.comparing(BiddingPostSummaryRecord::quoteCount)
				    .thenComparing(BiddingPostSummaryRecord::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
			    .toList();
		} else {
		    summaries = summaries.stream()
			    .sorted(Comparator.comparing(BiddingPostSummaryRecord::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
				    .reversed())
			    .toList();
		}

		int start = Math.toIntExact(pageable.getOffset());
		int end = Math.min(start + pageable.getPageSize(), summaries.size());
		List<BiddingPostSummaryRecord> content = start >= end ? List.of() : summaries.subList(start, end);
		return new PageImpl<>(content, pageable, summaries.size());
	    }

	public QuoteResponseRecord submitQuote(Long postId, QuoteCreateRequest request) {
		UserEntity workshop = getCurrentUser();
		BiddingPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("Post not found"));
		if (!PostStatus.OPEN.equals(post.getStatus())) {
			throw new IllegalStateException("Post is not open");
		}
		quoteRepository.findByPostIdAndWorkshopId(post.getId(), workshop.getId())
				.ifPresent(existing -> {
					throw new IllegalStateException("Quote already submitted");
				});

		QuoteEntity quote = QuoteEntity.builder()
				.post(post)
				.workshop(workshop)
				.offeredPrice(request.offeredPrice())
				.estimateDays(request.estimateDays())
				.status(QuoteStatus.PENDING)
				.build();
		return toQuoteResponse(quoteRepository.save(quote));
	}

	public QuoteResponseRecord updateQuote(Long quoteId, QuoteUpdateRequest request) {
		UserEntity workshop = getCurrentUser();
		QuoteEntity quote = getQuoteForWorkshop(quoteId, workshop.getId());
		if (!QuoteStatus.PENDING.equals(quote.getStatus())) {
			throw new IllegalStateException("Quote is not pending");
		}
		quote.setOfferedPrice(request.offeredPrice());
		quote.setEstimateDays(request.estimateDays());
		quoteRepository.save(quote);
		return toQuoteResponse(quote);
	}

	public QuoteResponseRecord withdrawQuote(Long quoteId) {
		UserEntity workshop = getCurrentUser();
		QuoteEntity quote = getQuoteForWorkshop(quoteId, workshop.getId());
		if (!QuoteStatus.PENDING.equals(quote.getStatus())) {
			throw new IllegalStateException("Quote is not pending");
		}
		quote.setStatus(QuoteStatus.REJECTED);
		quoteRepository.save(quote);
		return toQuoteResponse(quote);
	}

	public QuoteResponseRecord getMyQuoteOnPost(Long postId) {
		UserEntity workshop = getCurrentUser();
		return quoteRepository.findByPostIdAndWorkshopId(postId, workshop.getId())
				.map(this::toQuoteResponse)
				.orElse(null);
	}

	public Page<QuoteResponseRecord> listMyQuotes(String status, Pageable pageable) {
		UserEntity workshop = getCurrentUser();
		if (status == null || status.isBlank()) {
			return quoteRepository.findByWorkshopId(workshop.getId(), pageable)
					.map(this::toQuoteResponse);
		}
		return quoteRepository.findByWorkshopIdAndStatus(
				workshop.getId(),
				parseQuoteStatus(status),
				pageable)
				.map(this::toQuoteResponse);
	}

	public Page<BiddingPostSummaryRecord> listAllPosts(String status, Pageable pageable) {
		return (status == null || status.isBlank()
				? postRepository.findAll(pageable)
				: postRepository.findByStatus(parsePostStatus(status), pageable))
				.map(this::toPostSummary);
	}

	public BiddingPostDetailRecord updatePostStatus(Long postId, UpdatePostStatusRequest request) {
		BiddingPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("Post not found"));
		post.setStatus(parsePostStatus(request.status()));
		postRepository.save(post);
		return buildPostDetail(post);
	}

	public Page<QuoteResponseRecord> listAllQuotes(String status, Pageable pageable) {
		return (status == null || status.isBlank()
				? quoteRepository.findAll(pageable)
				: quoteRepository.findByStatus(parseQuoteStatus(status), pageable))
				.map(this::toQuoteResponse);
	}

	private UserEntity getCurrentUser() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private BiddingPostEntity getPostForCustomer(Long postId) {
		UserEntity customer = getCurrentUser();
		BiddingPostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("Post not found"));
		if (post.getCustomer() == null || !post.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission for post");
		}
		return post;
	}

	private QuoteEntity getQuoteForWorkshop(Long quoteId, Long workshopId) {
		QuoteEntity quote = quoteRepository.findById(quoteId)
				.orElseThrow(() -> new IllegalArgumentException("Quote not found"));
		if (quote.getWorkshop() == null || !quote.getWorkshop().getId().equals(workshopId)) {
			throw new IllegalStateException("No permission for quote");
		}
		return quote;
	}

	private void ensurePostEditable(BiddingPostEntity post) {
		if (!PostStatus.OPEN.equals(post.getStatus())) {
			throw new IllegalStateException("Post is not open");
		}
		boolean hasAccepted = quoteRepository.findByPostId(post.getId()).stream()
				.anyMatch(quote -> QuoteStatus.ACCEPTED.equals(quote.getStatus()));
		if (hasAccepted) {
			throw new IllegalStateException("Post already has accepted quote");
		}
	}

	private void saveAttachments(BiddingPostEntity post, List<BiddingAttachmentRequestRecord> attachments) {
		if (attachments == null || attachments.isEmpty()) {
			return;
		}
		for (BiddingAttachmentRequestRecord attachment : attachments) {
			BiddingAttachmentEntity entity = BiddingAttachmentEntity.builder()
					.post(post)
					.fileUrl(attachment.fileUrl())
					.fileType(attachment.fileType())
					.build();
			attachmentRepository.save(entity);
		}
	}

	private BiddingPostDetailRecord buildPostDetail(BiddingPostEntity post) {
		List<BiddingAttachmentResponseRecord> attachments = attachmentRepository.findByPostId(post.getId())
				.stream()
				.map(item -> new BiddingAttachmentResponseRecord(item.getId(), item.getFileUrl(), item.getFileType()))
				.toList();
		long quoteCount = quoteRepository.countByPostId(post.getId());
		String customerName = post.getCustomer() != null ? post.getCustomer().getFullName() : null;
		return new BiddingPostDetailRecord(
				post.getId(),
				post.getTitle(),
				post.getDescription(),
				post.getAiImageUrl(),
				post.getFrontDesignUrl(),
				post.getBackDesignUrl(),
				post.getStatus() != null ? post.getStatus().name() : null,
				post.getCustomer() != null ? post.getCustomer().getId() : null,
				customerName,
				quoteCount,
				attachments,
				post.getCreatedAt());
	}

	private List<BiddingPostSummaryRecord> toPostSummaries(List<BiddingPostEntity> posts) {
		List<BiddingPostSummaryRecord> result = new ArrayList<>();
		for (BiddingPostEntity post : posts) {
			result.add(toPostSummary(post));
		}
		return result;
	}

	private BiddingPostSummaryRecord toPostSummary(BiddingPostEntity post) {
		long quoteCount = quoteRepository.countByPostId(post.getId());
		return new BiddingPostSummaryRecord(
				post.getId(),
				post.getTitle(),
				post.getDescription(),
				post.getStatus() != null ? post.getStatus().name() : null,
				quoteCount,
				post.getCreatedAt());
	}

	private List<QuoteResponseRecord> toQuoteResponses(List<QuoteEntity> quotes) {
		return quotes.stream().map(this::toQuoteResponse).toList();
	}

	private QuoteResponseRecord toQuoteResponse(QuoteEntity quote) {
		UserEntity workshop = quote.getWorkshop();
		BiddingPostEntity post = quote.getPost();
		UserEntity customer = post != null ? post.getCustomer() : null;
		return new QuoteResponseRecord(
				quote.getId(),
				post != null ? post.getId() : null,
				post != null ? post.getTitle() : null,
				customer != null ? customer.getFullName() : null,
				workshop != null ? workshop.getId() : null,
				workshop != null ? workshop.getFullName() : null,
				workshop != null ? workshop.getAvatarUrl() : null,
				quote.getOfferedPrice(),
				quote.getEstimateDays(),
				quote.getStatus() != null ? quote.getStatus().name() : null,
				quote.getCreatedAt(),
				quote.getUpdatedAt());
	}

	private boolean containsIgnoreCase(String value, String keyword) {
		if (value == null || keyword == null) {
			return false;
		}
		return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

	private PostStatus parsePostStatus(String status) {
		return PostStatus.valueOf(status.toUpperCase(Locale.ROOT));
	}

	private QuoteStatus parseQuoteStatus(String status) {
		return QuoteStatus.valueOf(status.toUpperCase(Locale.ROOT));
	}

	private BiddingDesignRecord toBiddingDesignRecord(BiddingDesignEntity design) {
		return new BiddingDesignRecord(
				design.getId(),
				design.getName(),
				design.getFrontDesignUrl(),
				design.getBackDesignUrl(),
				design.getCreatedAt());
	}
}
