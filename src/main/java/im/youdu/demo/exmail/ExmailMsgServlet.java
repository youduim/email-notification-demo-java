package im.youdu.demo.exmail;

import im.youdu.sdk.client.AppClient;
import im.youdu.sdk.entity.YDApp;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ExmailMsgServlet  extends HttpServlet {
    private final static Logger log = Logger.getLogger(ExmailMsgServlet.class.getName());

    private String initErrMsg = "";
    private AppClient ydAppClient;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String respMsg = "ok";
        for(;;){
            if(null == ydAppClient){
                respMsg = "有度服务信息未初始化:"+initErrMsg;
                break;
            }

            String msgType = req.getParameter("msgType");
            if(null == msgType){
                respMsg = "消息类型为空";
                break;
            }

            if("newMail".equals(msgType)){
                doNewMailSend(req);
                break;
            }
            if("unRead".equals(msgType)){
                doUnReadMailSend(req);
                break;
            }
            respMsg = "未知的消息类型："+msgType;
            break;
        }
        //TODO 以上代码是示例伪代码，具体情况请根据您企业邮服务商的规范来读取新邮件的推送
        log.info(respMsg);
        OutputStream out = resp.getOutputStream();
        out.write(respMsg.getBytes());
        out.flush();
        out.close();
    }

    @Override
    public void init() throws ServletException {
        Properties prop = new Properties();
        InputStream in = null;
        try {
            in = this.getClass().getResourceAsStream("/ydapp.properties");
            prop.load(in);
            String buinStr = prop.getProperty("buin");
            int buin = Integer.valueOf(buinStr);
            String host = prop.getProperty("host");
            String appId = prop.getProperty("appId");
            String aesKey = prop.getProperty("appAESKey");
            String fmtStr = String.format("{buin:%d; host:%s; appId:%s; appAESKey: %s}", buin, host, appId, aesKey);
            log.info("读取到有度企业邮应用配置:"+fmtStr);

            YDApp ydApp = new YDApp();
            ydApp.setBuin(buin);
            ydApp.setHost(host);
            ydApp.setAppId(appId);
            ydApp.setAppAesKey(aesKey);
            ydAppClient = new AppClient(ydApp);
        } catch (Exception e) {
            initErrMsg = "读取有度企业邮应用配置发生错误: "+e.getMessage();
            log.error(initErrMsg);
        }
    }

    private void doNewMailSend(HttpServletRequest req){
        String title = req.getParameter("title");
        String toUser = req.getParameter("toUser");
        String fromUser = req.getParameter("fromUser");
        String mailLink = req.getParameter("mailLink");
        String timeStr = req.getParameter("time");
        Long sendTime = Long.valueOf(timeStr);
        try {
            ydAppClient.sendNewMailMsg(toUser, fromUser, title, mailLink, sendTime);
        } catch (Exception e) {
            log.error("推送邮件到有度异常："+e.getMessage());
        }
    }

    private void doUnReadMailSend(HttpServletRequest req){
        String toUser = req.getParameter("toUser");
        String unReadStr = req.getParameter("unReadCount");
        Integer unReadCount = Integer.valueOf(unReadStr);
        String timeStr = req.getParameter("time");
        Long sendTime = Long.valueOf(timeStr);
        try {
            ydAppClient.sendUnreadMailMsg(toUser, unReadCount, sendTime);
        } catch (Exception e) {
            log.error("推送邮件到有度异常："+e.getMessage());
        }
    }

}
