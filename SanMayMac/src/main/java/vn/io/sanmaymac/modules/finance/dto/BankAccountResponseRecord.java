package vn.io.sanmaymac.modules.finance.dto;

public record BankAccountResponseRecord(
        Long id,
        String bankName,
        String accountNo,
        String accountName,
        Boolean isVerified) {
}
