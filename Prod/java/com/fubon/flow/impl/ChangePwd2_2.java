package com.fubon.flow.impl;

import com.neux.garden.authorization.LoginUserBean;
import com.neux.garden.dbmgr.DaoFactory;
import com.fubon.flow.ILogic;
import com.fubon.utils.FlowUtils;
import com.fubon.utils.MessageUtils;
import com.fubon.utils.ProjUtils;
import com.fubon.utils.bean.MailBean;
import com.fubon.utils.bean.OTPBean;
import com.fubon.utils.bean.SMSBean;
import com.fubon.webservice.WebServiceAgent;
import com.fubon.webservice.bean.RQBean;
import com.fubon.webservice.bean.RSBean;
import com.neux.utility.orm.bean.DataObject;
import com.neux.utility.orm.dal.dao.module.IDao;
import com.neux.utility.utils.PropertiesUtil;
import com.neux.utility.utils.date.DateUtil;
import com.neux.utility.utils.jsp.info.JSPQueryStringInfo;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Titan
 * Date: 2016/5/19
 * Time: 下午 8:20
 * To change this template use File | Settings | File Templates.
 */
public class ChangePwd2_2 implements ILogic {
    @Override
    public void getDraftData(JSONObject content, Document draftData, JSPQueryStringInfo queryStringInfo) throws Exception {

        LoginUserBean loginUserBean = ProjUtils.getLoginBean(queryStringInfo.getRequest().getSession());
        String userId = loginUserBean.getUserId();

        IDao dao = DaoFactory.getDefaultDao();

        //取得是否已撥款
        String isRecord = ProjUtils.isPayHistory(userId,dao) ? "Y" : "N";
        String mobile = "" , email = "";
       
       
        
        

        //取得登入者手機跟Email
        mobile = loginUserBean.getCustomizeValue("AplyCellPhoneNo");
        email = loginUserBean.getCustomizeValue("AplyEmail");
        
        
        //////////10/05  手機改詢問390    
        
        String env = PropertiesUtil.loadPropertiesByClassPath("/config.properties").getProperty("env");
        if(!"sit".equalsIgnoreCase(env)) {
            RQBean rqBean54 = new RQBean();
            rqBean54.setTxId("EB032154");
            rqBean54.addRqParam("CUST_NO",userId);

            RSBean rsBean54 = WebServiceAgent.callWebService(rqBean54);

            if(rsBean54.isSuccess()) {
                Document doc = DocumentHelper.parseText(rsBean54.getTxnString());

                String cellPhone = ProjUtils.get032154Col(doc,"8001");

                //行動電話抓8001
                if(StringUtils.isNotEmpty(cellPhone)) {
                    mobile = cellPhone;
                }

            }
        }

        //取得OTP驗證碼
        OTPBean otpBean = ProjUtils.createOTP(queryStringInfo.getRequest());

        //須判斷會員狀態
        //如果有撥款，發簡訊
        if("Y".equalsIgnoreCase(isRecord)) {

            SMSBean smsBean = new SMSBean();
            smsBean.setMobile(mobile);
            smsBean.setTitle(MessageUtils.OTPTitle);
            smsBean.setContent(MessageUtils.toOTPContent(otpBean.getOtpNumber(),otpBean.getOtpCode()));

            MessageUtils.sendSMS(smsBean);

            MessageUtils.saveOTPLog(dao,mobile,null,queryStringInfo.getRequest(),otpBean.getOtpNumber(),otpBean.getOtpCode(),smsBean.getContent(),userId);
        }
        //如果沒撥款，發Email
        else {

            MailBean mailBean = new MailBean("otp");
            mailBean.setTitle(MessageUtils.OTPTitle);
            mailBean.setReceiver(email);
            mailBean.addResultParam("otpNumber", otpBean.getOtpNumber());
            mailBean.addResultParam("otpCode", otpBean.getOtpCode());
            mailBean.addResultParam("otpTime", DateUtil.convert14ToDate("yyyy/MM/dd HH:mm:ss",otpBean.getOtpTime()));
            mailBean.addResultParam("funcName","變更代碼/密碼");

            MessageUtils.sendEmail(mailBean,userId);

            MessageUtils.saveOTPLog(dao,null,email,queryStringInfo.getRequest(),otpBean.getOtpNumber(),otpBean.getOtpCode(),null,userId);
        }



        content.put("hasAppropriation",isRecord);
        content.put("mobile",mobile);
        content.put("email",email);
        content.put("code_img",otpBean.getCodeImg());

    }

    @Override
    public void doAction(JSPQueryStringInfo queryStringInfo,JSONObject content) throws Exception {
        ProjUtils.validOTP(queryStringInfo,content);
    }
}