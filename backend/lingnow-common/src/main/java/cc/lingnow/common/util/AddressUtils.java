package cc.lingnow.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 获取地址类
 *
 * @author LingNow Team
 */
@Slf4j
public class AddressUtils {

    // IP地址查询
    public static final String IP_URL = "http://whois.pconline.com.cn/ipJson.jsp";

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        String address = UNKNOWN;
        // 内网不查询
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }
        // 暂时屏蔽外网查询，防止超时导致 502
        return address;
        /*
        try {
            String rspStr = HttpUtil.get(IP_URL + "?ip=" + ip + "&json=true", 3000);
            if (StrUtil.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            JSONObject obj = JSONUtil.parseObj(rspStr);
            String region = obj.getStr("pro");
            String city = obj.getStr("city");
            return String.format("%s %s", region, city);
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip);
        }
        return address;
        */
    }
}
