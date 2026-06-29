package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record WalletResponseRecord(
        Long userId,
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        Integer aiTokenBalance) {
}
