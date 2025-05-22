package com.nsfw.image;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ImageCensorController {
    @PostMapping("/classify")
    public Map<String, Object> classify(@RequestParam("image") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用绝对路径保存上传目录
            String uploadDirPath = System.getProperty("user.dir") + "/uploads/";
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs(); // 创建目录
            }

            // 使用 UUID 防止重名
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File savedFile = new File(uploadDir, filename);
            file.transferTo(savedFile);

            // 预测
            Map<String, Float> prediction = ImageCensor.predict(savedFile.getAbsolutePath());

            // 找出最大概率分类
            String label = prediction.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            result.put("success", true);
            result.put("label", label);
            result.put("probabilities", prediction);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

}
