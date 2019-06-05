package com.ddlab.rnd.controller;

import com.ddlab.rnd.service.ISigntureService;
import com.ddlab.rnd.utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 功能说明 ：
 *
 * @ version Rversion 1.0.0
 * 修改时间       | 修改内容
 */
@RestController
public class SignatureController {

    @Autowired
    private ISigntureService signtureService;

    @PostMapping(value = "sign")
    public void sign (HttpServletRequest request, HttpServletResponse response) {
        // 获取到请求的xml数据
        String xmlString = HttpUtil.getBodyString(request);
        String ceb = request.getParameter("ceb");
        if(!StringUtils.isEmpty(ceb) && "1".equals(ceb)) {
            signtureService.signCeb(xmlString, response);
        } else {
            signtureService.sign(xmlString, response, false);
        }

    }
}
