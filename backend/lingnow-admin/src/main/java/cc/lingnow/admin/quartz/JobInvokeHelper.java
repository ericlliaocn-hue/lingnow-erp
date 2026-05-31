package cc.lingnow.admin.quartz;

import cc.lingnow.common.exception.BusinessException;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 安全调用定时任务目标 Bean。
 */
@Component
public class JobInvokeHelper {

    private static final Pattern BEAN_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    public void validate(String invokeTarget) {
        ParsedTarget target = parse(invokeTarget);
        Object bean = SpringContextHolder.getBean(target.beanName());
        if (bean == null) {
            throw new BusinessException("调用目标 Bean 不存在");
        }
        findMethod(bean, target);
    }

    public Object invoke(String invokeTarget) throws Exception {
        ParsedTarget target = parse(invokeTarget);
        Object bean = SpringContextHolder.getBean(target.beanName());
        if (bean == null) {
            throw new BusinessException("调用目标 Bean 不存在");
        }
        Method method = findMethod(bean, target);
        return method.invoke(bean, convertArgs(method.getParameterTypes(), target.args()));
    }

    private ParsedTarget parse(String invokeTarget) {
        if (StrUtil.isBlank(invokeTarget) || invokeTarget.contains("..") || invokeTarget.contains("#")) {
            throw new BusinessException("调用目标格式错误");
        }
        String target = invokeTarget.trim();
        int dotIndex = target.indexOf('.');
        if (dotIndex <= 0) {
            throw new BusinessException("调用目标格式应为 beanName.methodName 或 beanName.methodName(args)");
        }

        String beanName = target.substring(0, dotIndex);
        String methodPart = target.substring(dotIndex + 1);
        String methodName;
        List<String> args = new ArrayList<>();

        int leftParen = methodPart.indexOf('(');
        if (leftParen >= 0) {
            int rightParen = methodPart.lastIndexOf(')');
            if (rightParen < leftParen || rightParen != methodPart.length() - 1) {
                throw new BusinessException("调用目标参数格式错误");
            }
            methodName = methodPart.substring(0, leftParen);
            String argText = methodPart.substring(leftParen + 1, rightParen).trim();
            if (StrUtil.isNotBlank(argText)) {
                args = parseArgs(argText);
            }
        } else {
            methodName = methodPart;
        }

        if (!BEAN_PATTERN.matcher(beanName).matches() || !METHOD_PATTERN.matcher(methodName).matches()) {
            throw new BusinessException("调用目标只允许调用 Spring Bean 的公开方法");
        }
        return new ParsedTarget(beanName, methodName, args);
    }

    private List<String> parseArgs(String argText) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < argText.length(); i++) {
            char c = argText.charAt(i);
            if (c == '\'') {
                quote = !quote;
                continue;
            }
            if (c == ',' && !quote) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (quote) {
            throw new BusinessException("调用目标参数引号不完整");
        }
        result.add(current.toString().trim());
        return result;
    }

    private Method findMethod(Object bean, ParsedTarget target) {
        for (Method method : bean.getClass().getMethods()) {
            if (!method.getName().equals(target.methodName())) {
                continue;
            }
            if (method.getParameterCount() != target.args().size()) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass() == Object.class) {
                continue;
            }
            ensureSupportedTypes(method.getParameterTypes());
            return method;
        }
        throw new BusinessException("调用目标方法不存在或参数不支持");
    }

    private void ensureSupportedTypes(Class<?>[] parameterTypes) {
        for (Class<?> type : parameterTypes) {
            if (!(type == String.class
                    || type == Integer.class || type == int.class
                    || type == Long.class || type == long.class
                    || type == Boolean.class || type == boolean.class
                    || type == Double.class || type == double.class)) {
                throw new BusinessException("调用目标只支持基础类型参数");
            }
        }
    }

    private Object[] convertArgs(Class<?>[] parameterTypes, List<String> args) {
        Object[] values = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            String value = args.get(i);
            Class<?> type = parameterTypes[i];
            if (type == String.class) values[i] = value;
            else if (type == Integer.class || type == int.class) values[i] = Integer.valueOf(value);
            else if (type == Long.class || type == long.class) values[i] = Long.valueOf(value);
            else if (type == Boolean.class || type == boolean.class) values[i] = Boolean.valueOf(value);
            else if (type == Double.class || type == double.class) values[i] = Double.valueOf(value);
            else throw new BusinessException("调用目标参数类型不支持");
        }
        return values;
    }

    private record ParsedTarget(String beanName, String methodName, List<String> args) {
    }
}
