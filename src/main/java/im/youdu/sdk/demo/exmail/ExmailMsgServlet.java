package im.youdu.sdk.demo.exmail;

import im.youdu.sdk.client.AppClient;
import im.youdu.sdk.entity.Const;
import im.youdu.sdk.entity.EmailBody;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExmailMsgServlet  extends HttpServlet {
    private final static Logger log = Logger.getLogger(ExmailMsgServlet.class.getName());

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private String initErrMsg = "";
    private AppClient ydAppClient;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String respMsg = "ok";
        if(null == ydAppClient){
                log.error("有度服务信息未初始化:"+initErrMsg);
                return;
        }
        try {
            this.parseRequest(req);
        } catch (Exception e) {
            respMsg = e.getMessage();
        }
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

    //TODO 这里只是一段示例代码, 具体实现请参照您企业邮箱服务商的接口规范
    private void parseRequest(HttpServletRequest req) throws Exception{
        String msgType = req.getParameter("msgType");
        if(null == msgType){
            throw new Exception("消息类型为空");
        }

        EmailBody email = new EmailBody();
        long sendTime = 0;
        String timeStr = req.getParameter("time");
        if(null != timeStr){
            sendTime = Long.valueOf(timeStr);
        }
        String toUser = req.getParameter("toUser");
        email.setTimex(sendTime);
        email.setToUser(toUser);

        if("newMail".equals(msgType)){
            String title = req.getParameter("title");
            String fromUser = req.getParameter("fromUser");
            String mailUrl = req.getParameter("mailUrl");

            email.setAction(Const.Email_Action_NewMail);
            email.setTitle(title);
            email.setFromUser(fromUser);
            email.setMailUrl(mailUrl);
        }else if("unread" .equals(msgType)){
            String unReadStr = req.getParameter("unReadCount");
            int unReadCount = Integer.valueOf(unReadStr);
            email.setAction(Const.Email_Action_UnRead);
            email.setUnread(unReadCount);
        }else{
            throw new Exception("位置的消息类型："+msgType);
        }
        MailTread thread = new MailTread(email);
        threadPool.execute(thread);
    }

    class MailTread implements Runnable{
         EmailBody email;

        public MailTread(EmailBody _email){
            this.email = _email;
        }

        @Override
        public void run() {
            try {
                ydAppClient.sendMailMsg (this.email);
            } catch (Exception e) {
                log.error("推送新邮件到有度异常："+e.getMessage());
            }
        }
    }

}
