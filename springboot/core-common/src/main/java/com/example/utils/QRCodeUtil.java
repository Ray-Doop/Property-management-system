package com.example.utils;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

public class QRCodeUtil {

    /**
     * 生成二维码并返回 Base64
     */
    public static String generateQRCodeBase64(String content) {
        try {
            QrConfig config = new QrConfig(300, 300);
            config.setMargin(2);
            config.setForeColor(Color.BLACK.getRGB());
            config.setBackColor(Color.WHITE.getRGB());

            BufferedImage image = QrCodeUtil.generate(content, config);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 解析二维码（从图片文件）
     */
    public static String decodeQRCode(InputStream inputStream) {
        try {
            return QrCodeUtil.decode(ImageIO.read(inputStream));
        } catch (Exception e) {
            throw new RuntimeException("解析二维码失败", e);
        }
    }
}
