package com.example.utils;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.IdUtil;

import java.awt.image.BufferedImage;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 验证码工具类
 */
public class CaptchaUtil {

    /**
     * 生成验证码图片（Base64格式）
     * @return 包含验证码图片Base64和验证码字符串的对象
     */
    public static CaptchaResult generateCaptcha() {
        // 定义验证码字符集（数字+字母，排除容易混淆的字符）
        String codeChars = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        RandomGenerator randomGenerator = new RandomGenerator(codeChars, 4);
        
        // 创建线干扰的验证码，宽120，高40，4位验证码，150条干扰线
        LineCaptcha lineCaptcha = new LineCaptcha(120, 40, 4, 150);
        lineCaptcha.setGenerator(randomGenerator);
        lineCaptcha.createCode();
        
        // 获取验证码字符串
        String code = lineCaptcha.getCode();
        
        // 生成唯一标识
        String captchaId = IdUtil.simpleUUID();
        
        // 将图片转换为Base64
        BufferedImage image = lineCaptcha.getImage();
        String base64Image = imageToBase64(image);
        
        CaptchaResult result = new CaptchaResult();
        result.setCaptchaId(captchaId);
        result.setImageBase64(base64Image);
        result.setCode(code);
        
        return result;
    }
    
    /**
     * 将BufferedImage转换为Base64字符串
     */
    private static String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("验证码图片转换失败", e);
        }
    }
    
    /**
     * 验证码结果类
     */
    public static class CaptchaResult {
        private String captchaId;
        private String imageBase64;
        private String code;
        
        public String getCaptchaId() {
            return captchaId;
        }
        
        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }
        
        public String getImageBase64() {
            return imageBase64;
        }
        
        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
    }
}

