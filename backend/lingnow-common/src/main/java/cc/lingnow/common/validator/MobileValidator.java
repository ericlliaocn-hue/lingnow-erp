package cc.lingnow.common.validator;

import cc.lingnow.common.annotation.Mobile;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 手机号码校验器
 *
 * @author LingNow Team
 */
public class MobileValidator implements ConstraintValidator<Mobile, String> {

    /**
     * 中国大陆手机号码正则
     * 规则：1开头，第二位3-9，后面9位数字
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public void initialize(Mobile constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果为空，由 @NotNull 或 @NotBlank 处理，这里默认通过
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return MOBILE_PATTERN.matcher(value).matches();
    }
}
