package com.nsfw.image;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImageCensorController {

    @PostMapping("/classify")
    public Map<String, Object> classify(@RequestParam("image") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 保存上传文件到临时目录
            File tempFile = File.createTempFile("upload_", ".jpg");
            file.transferTo(tempFile);

            // 预测
            Map<String, Float> prediction = ImageCensor.predict(tempFile.getAbsolutePath());

            // 找出最大概率分类
            String label = prediction.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            result.put("success", true);
            result.put("label", label);
            result.put("probabilities", prediction);
            tempFile.delete(); // 删除临时文件
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
