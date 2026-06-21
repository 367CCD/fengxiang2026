package com.txq.xiaohongshu.utils;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.AbstractCaptcha;

import java.awt.*;
import java.io.OutputStream;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码工具类 — 基于 HuTool 生成图片验证码
 *
 * <pre>
 * 使用示例：
 *   // 1. 生成线段干扰验证码
 *   LineCaptcha captcha = CaptchaUtils.createLineCaptcha();
 *   String code = CaptchaUtils.getCode(captcha);   // 验证码文本
 *   String img  = CaptchaUtils.toBase64(captcha);   // Base64 图片
 *
 *   // 2. 直接写入 HttpServletResponse
 *   CaptchaUtils.write(captcha, response.getOutputStream());
 * </pre>
 */
public class CaptchaUtils {

    // ==================== 默认参数 ====================
    private static final int WIDTH            = 120;
    private static final int HEIGHT           = 44;
    private static final int CODE_COUNT       = 4;
    private static final int LINE_COUNT       = 20;
    private static final int CIRCLE_COUNT     = 6;
    private static final int SHEAR_THICKNESS  = 4;

    private static final Font FONT = new Font("Arial", Font.BOLD, 28);

    // ==================== 线段干扰验证码 ====================

    /** 默认参数: 120×44, 4位, 20条干扰线 */
    public static LineCaptcha createLineCaptcha() {
        return createLineCaptcha(WIDTH, HEIGHT, CODE_COUNT, LINE_COUNT);
    }

    public static LineCaptcha createLineCaptcha(int width, int height, int codeCount, int lineCount) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(width, height, codeCount, lineCount);
        captcha.setFont(FONT);
        captcha.createCode();
        return captcha;
    }

    // ==================== 圆圈干扰验证码 ====================

    /** 默认参数: 120×44, 4位, 6个干扰圈 */
    public static CircleCaptcha createCircleCaptcha() {
        return createCircleCaptcha(WIDTH, HEIGHT, CODE_COUNT, CIRCLE_COUNT);
    }

    public static CircleCaptcha createCircleCaptcha(int width, int height, int codeCount, int circleCount) {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(width, height, codeCount, circleCount);
        captcha.setFont(FONT);
        captcha.createCode();
        return captcha;
    }

    // ==================== 扭曲干扰验证码 ====================

    /** 默认参数: 120×44, 4位, 4px扭曲线 */
    public static ShearCaptcha createShearCaptcha() {
        return createShearCaptcha(WIDTH, HEIGHT, CODE_COUNT, SHEAR_THICKNESS);
    }

    public static ShearCaptcha createShearCaptcha(int width, int height, int codeCount, int thickness) {
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(width, height, codeCount, thickness);
        captcha.setFont(FONT);
        captcha.createCode();
        return captcha;
    }

    // ==================== 随机验证码（混合三种类型） ====================

    /** 随机选择一种验证码类型（线段 / 圆圈 / 扭曲） */
    public static AbstractCaptcha createCaptcha() {
        return createCaptcha(WIDTH, HEIGHT, CODE_COUNT, LINE_COUNT, CIRCLE_COUNT, SHEAR_THICKNESS);
    }

    /**
     * 随机选择一种验证码类型
     *
     * @param width        宽度
     * @param height       高度
     * @param codeCount    验证码字符个数
     * @param lineCount    干扰线条数（选中线段类型时生效）
     * @param circleCount  干扰圆圈数（选中圆圈类型时生效）
     * @param thickness    扭曲厚度   （选中扭曲类型时生效）
     */
    public static AbstractCaptcha createCaptcha(int width, int height, int codeCount,
                                                 int lineCount, int circleCount, int thickness) {
        int type = ThreadLocalRandom.current().nextInt(3);
        return switch (type) {
            case 0 -> createLineCaptcha(width, height, codeCount, lineCount);
            case 1 -> createCircleCaptcha(width, height, codeCount, circleCount);
            default -> createShearCaptcha(width, height, codeCount, thickness);
        };
    }

    // ==================== 通用便捷方法 ====================

    /** 获取验证码文本 */
    public static String getCode(AbstractCaptcha captcha) {
        return captcha.getCode();
    }

    /** 获取 Base64 编码图片（可直接拼到 &lt;img src="data:image/png;base64,…"&gt;） */
    public static String toBase64(AbstractCaptcha captcha) {
        return captcha.getImageBase64();
    }

    /** 将验证码图片写入输出流 */
    public static void write(AbstractCaptcha captcha, OutputStream out) {
        captcha.write(out);
    }

}