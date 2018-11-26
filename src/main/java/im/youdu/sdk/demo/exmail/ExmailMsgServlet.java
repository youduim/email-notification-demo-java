package im.youdu.sdk.demo.exmail;

import im.youdu.sdk.client.AppClient;
import im.youdu.sdk.client.MailMsgClient;
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
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExmailMsgServlet  extends HttpServlet {
    private final static Logger log = Logger.getLogger(ExmailMsgServlet.class.getName());

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private String initErrMsg = "";
    private MailMsgClient mailMsgClient;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String respMsg = "ok";
        if(null == mailMsgClient){
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
            String exmailAppAESKey = prop.getProperty("exmailAppAESKey");
            String fmtStr = String.format("{buin:%d; host:%s; appAESKey: %s}", buin, host, exmailAppAESKey);
            log.info("读取到有度企业邮应用配置:"+fmtStr);
            mailMsgClient = new MailMsgClient(buin, host, exmailAppAESKey);
        } catch (Exception e) {
            initErrMsg = "读取有度企业邮应用配置发生错误: "+e.getMessage();
            log.error(initErrMsg);
        }
    }

    private void parseRequest(HttpServletRequest req){
        String toUser = "test1";
//        String toEmail = "test1@test.com";
        EmailBody email = new EmailBody();
        email.setFromUser("test2");
//        email.setFromEmail("test2@test.com");
        email.setSubject("测试邮件消息");
        email.setAction(Const.Mail_Msg_New);
        email.setTimex(Calendar.getInstance().getTimeInMillis());
        MailTread thread = new MailTread(toUser, "", email);
        threadPool.execute(thread);
    };

    //TODO 这里是一段示例代码, 具体实现请参照您企业邮箱服务商的接口规范
    private void parseRequest_demo(HttpServletRequest req) throws Exception{
        String msgType = req.getParameter("msgType");
        if(null == msgType){
            throw new Exception("消息类型为空");
        }
        String toUser = req.getParameter("toUser");

        EmailBody email = new EmailBody();
        long sendTime = 0;
        String timeStr = req.getParameter("time");
        if(null != timeStr){
            sendTime = Long.valueOf(timeStr);
        }else{
            sendTime = Calendar.getInstance().getTimeInMillis();
        }
        email.setTimex(sendTime);
        if("newMail".equals(msgType)){
            String subject = req.getParameter("subject");
            String fromUser = req.getParameter("fromUser");
            String mailLink = req.getParameter("mailLink");

            email.setAction(Const.Mail_Msg_New);
            email.setSubject(subject);
            email.setFromUser(fromUser);
            email.setLink(mailLink);
        }else if("unread" .equals(msgType)){
            String unReadStr = req.getParameter("unReadCount");
            int unReadCount = Integer.valueOf(unReadStr);
            email.setAction(Const.Mail_Msg_Unread);
            email.setUnreadCount(unReadCount);
        }else{
            throw new Exception("位置的消息类型："+msgType);
        }
        MailTread thread = new MailTread(toUser, "", email);
        threadPool.execute(thread);
    }

    class MailTread implements Runnable{
        String toUser;
        String toEmail;
        EmailBody email;

        public MailTread(String _toUser, String _toEmail, EmailBody _email){
            this.toUser = _toUser;
            this.toEmail = _toEmail;
            this.email = _email;
        }

        @Override
        public void run() {
            try {
                mailMsgClient.sendMailMsg (toUser,toEmail,this.email);
            } catch (Exception e) {
                log.error("推送新邮件到有度异常："+e.getMessage());
            }
        }
    }

}
