package com.ddlab.rnd.service;

import javax.servlet.http.HttpServletResponse;

/**
 * 功能说明 ：
 *
 * @ version Rversion 1.0.0
 * 修改时间       | 修改内容
 */
public interface ISigntureService {

    void sign(String xmlStr, HttpServletResponse response, boolean cebRequest);

    void signCeb(String xmlStr, HttpServletResponse response);
}
