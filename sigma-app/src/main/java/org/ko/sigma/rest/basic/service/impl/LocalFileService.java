package org.ko.sigma.rest.basic.service.impl;

import io.github.sigmaol.help.Base64Helper;
import io.github.sigmaol.web.api.ResponseCode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ko.sigma.core.bean.FileInfo;
import org.ko.sigma.core.exception.BusinessException;
import org.ko.sigma.core.properties.FileProperties;
import org.ko.sigma.rest.basic.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Date;

@Service
public class LocalFileService implements FileService {

    @Autowired
    private FileProperties fileProperties;

    private static final Logger logger = LoggerFactory.getLogger(LocalFileService.class);

    /**
     * <p>文件上传</p>
     * @param file
     * @return
     */
    @Override
    public FileInfo upload(MultipartFile file) {
        logger.info("file info name[{}], originalFileName[{}], size[{}]",
                file.getName(),
                file.getOriginalFilename(),
                file.getSize());
        String fileName = new Date().getTime() +
                file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

        //如果没有目录则创建
        File folder = new File(fileProperties.getFolder());
        if (!folder.exists()) folder.mkdir();

        File localFile = new File(fileProperties.getFolder(), fileName);
        try {
            //从请求中将文件拷贝到本地
            file.transferTo(localFile);
            //上传至其他地方
//            file.getInputStream();
        } catch (IOException e) {
            logger.error("org.ko.prototype.core.service.impl.LocalFileService#upload exception: {}", e.getMessage());
        }
        return new FileInfo(Base64Helper.encodeToString(fileName.getBytes()), file.getOriginalFilename());
    }

    /**
     * <p>文件下载</p>
     * @param id
     * @param request
     */
    @Override
    public void download(String id, String name, ServletWebRequest request) {
        String fileId = Base64Helper.decode(id);
        assert request.getResponse() != null;
        try (
                InputStream inputStream =
                        new FileInputStream(new File(fileProperties.getFolder(), fileId));
                OutputStream outputStream = request.getResponse().getOutputStream()) {
            request.getResponse().setContentType("application/x-download");
            request.getResponse().addHeader("Content-Disposition", "attachment;filename="
                    + (StringUtils.isNotBlank(name) ? name : fileId));
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("org.ko.prototype.core.service.impl.LocalFileService#download exception: {}", e.getMessage());
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }
}