package com.champ.download.ctrl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : championjing
 * @version V1.0
 * @Description:
 * @date Date : 2021年03月17日 9:30
 */
@RestController
@RequestMapping("/file")
public class FileCtrl {

    private static final Logger log = LoggerFactory.getLogger(FileCtrl.class);

    private final static int BUFFER_SIZE = 1024 * 1024;

    @RequestMapping(value = "/{filename}", method = RequestMethod.HEAD)
    public Map head(@PathVariable String filename, HttpServletResponse resp) {
        log.info("查询文件是否存在");
        String filePath = "D:\\迅雷下载\\" + filename;
        File file = new File(filePath);
        Map map = new HashMap();
        if (file.exists()) {
            map.put("length", file.length());
        } else {
            resp.setStatus(HttpStatus.NOT_FOUND.value());
        }
        return map;
    }

    @GetMapping("/{filename}")
    public void download(@PathVariable String filename,
                         HttpServletRequest req,
                         HttpServletResponse resp) throws IOException {
        //断点续传
        //todo 文件信息做缓存能减少查数据次数
        String filePath = "D:\\迅雷下载\\" + filename;
        File file = new File(filePath);
        long length = file.length();
        long lastModifiedLong = file.lastModified();
        String rangeStr = req.getHeader(HttpHeaders.RANGE);

        resp.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
        resp.setHeader(HttpHeaders.LAST_MODIFIED, String.valueOf(lastModifiedLong));
        resp.setHeader(HttpHeaders.CONNECTION, "Keep-Alive");
//        resp.setHeader( HttpHeaders.ETAG, "" ); todo

        //todo 考虑将文件管理的实现类的读取换成RandomAccessFile
//        RandomAccessFile raf = new RandomAccessFile( file, "r" );
        InputStream is = new FileInputStream(file);
        ServletOutputStream os = resp.getOutputStream();
        //判断是第一次请求，还是后续的请求

        if (StringUtils.isBlank(rangeStr)) {
            //第一次收到请求，提取文件信息，返回给浏览器
            log.info("请求中没有range头信息");
            resp.setStatus(HttpStatus.OK.value());
            resp.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        } else {
            log.info("range信息:{}", rangeStr);
            //有range 返回206
            resp.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            RangeHeader rangeHeader = new RangeHeader(rangeStr, length);
            String contentRange = rangeHeader.toContentRange();
            resp.setHeader(HttpHeaders.CONTENT_RANGE, contentRange);
            //下面读取的内容的长度与range的end没有对比，会不会有影响
            // todo 如果请求头中有end信息，是否控制读取的次数
            is.skip(rangeHeader.getStart());
//            if ( rangeHeader.getEnd()+1 == rangeHeader.getFileLength() ) {
//                while (is.read(bs) != -1) {
//                    os.write( bs );
//                }
//            } else {
//                long len = rangeHeader.getEnd() - rangeHeader.getStart();
//                long tail = len%BUFFER_SIZE;
//                long count = len/BUFFER_SIZE;
//                while( is.read(bs) != -1 && count>0) {
//                    os.write( bs );
//                    count--;
//                    log.info("使用count读取的次数:{}",count);
//                    if ( count == 0L && tail != 0L) {
//                        count++;
//                        int tailBuffer = (int) tail;
//                        bs = new byte[ Integer.valueOf( tailBuffer ) ];
//                        tail = 0L;
//                        log.info("对尾部进行读取:{}",tail);
//                    }
//                }
//            }
        }
        byte[] bs = new byte[BUFFER_SIZE];
        while (is.read(bs) != -1) {
            os.write(bs);
        }
        os.close();
        is.close();

    }

    class RangeHeader {
        private final String PREFIX = "bytes";
        private final String EQUAL = "=";
        private final String GAP = "-";
        private final String SLASH = "/";

        private Long start;
        private Long end;
        private Long fileLength;

        /**
         * 请求头转换为相应的Content-Range
         *
         * @param rangeStr
         * @param fileLength
         */
        public RangeHeader(String rangeStr, long fileLength) {
            this.start = null;
            this.end = null;
            this.fileLength = fileLength;
            String range = rangeStr.replace(PREFIX + EQUAL, "");
            String[] strs = range.split(GAP);
            //start
            if (!StringUtils.isBlank(strs[0])) {
                this.start = Long.parseLong(strs[0].trim());
            }
            // end
            if (strs.length == 2 && !StringUtils.isBlank(strs[1])) {
                this.end = Long.parseLong(strs[1].trim());
            }
        }

        /**
         * 产生 Content-Range 头信息
         *
         * @return
         */
        public String toContentRange() {
            StringBuilder sb = new StringBuilder(PREFIX);
            sb.append(" ");
            sb.append(this.start == null ? 0 : this.start);
            sb.append(GAP);
            sb.append(this.end == null || this.end >= this.fileLength ? (this.fileLength - 1) : this.end);
            sb.append(SLASH);
            sb.append(this.fileLength);
            return sb.toString();
        }

        public Long getStart() {
            return start;
        }

        public void setStart(Long start) {
            this.start = start;
        }

        public Long getEnd() {
            return end;
        }

        public void setEnd(Long end) {
            this.end = end;
        }

        public Long getFileLength() {
            return fileLength;
        }

        public void setFileLength(Long fileLength) {
            this.fileLength = fileLength;
        }
    }
}
