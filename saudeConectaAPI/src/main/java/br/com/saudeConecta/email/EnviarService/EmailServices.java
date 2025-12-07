package br.com.saudeConecta.email.EnviarService;

import br.com.saudeConecta.infra.exceptions.EmailServiceException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;


@Slf4j
@Service
public class EmailServices {


    @Autowired
    private JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailServices(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void enviarEmail(String destinatario, String assunto, String corpo) throws EmailServiceException {
        log.info("Iniciando envio de e-mail para: {}", destinatario);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(assunto);
            message.setText(corpo);
            mailSender.send(message);
            log.info("E-mail enviado com sucesso para: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail para: {} - Erro: {}", destinatario, e.getMessage());
            throw new EmailServiceException("Erro ao enviar e-mail");
        }
    }






    public void enviarEmailComPaginaHTML(String to, String subject, String templateName, Map<String, Object> model) throws MessagingException {
        log.info("Enviando e-mail HTML para: {} com template: {}", to, templateName);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setTo(to);
            helper.setSubject(subject);
            String htmlContent = templateEngine.process(templateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("E-mail HTML enviado com sucesso para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail HTML para: {} - Erro: {}", to, e.getMessage());
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }


    public void enviarEmailComLoginPaciente(String paciEmail, String LoginDeUsuario , String TemplateName, Map<String, Object> model) throws MessagingException {
        log.info("Enviando e-mail de login para paciente: {}", paciEmail);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setTo(paciEmail);
            helper.setSubject(LoginDeUsuario);
            String htmlContent = templateEngine.process(TemplateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("E-mail de login enviado com sucesso para paciente: {}", paciEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail de login para paciente: {} - Erro: {}", paciEmail, e.getMessage());
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }

    public void enviarEmailComLoginMedico(String medEmail, String loginDeUsuario, String TemplateName, Map<String, Object> model) throws MessagingException {
        log.info("Enviando e-mail de login para medico: {}", medEmail);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setTo(medEmail);
            helper.setSubject(loginDeUsuario);
            String htmlContent = templateEngine.process(TemplateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("E-mail de login enviado com sucesso para medico: {}", medEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail de login para medico: {} - Erro: {}", medEmail, e.getMessage());
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }

    public void enviarLembreteDeAlertaParaPaciente(String paciEmail, String loginDeUsuario, String TemplateName, Map<String, Object> model) throws MessagingException {
        log.info("Enviando lembrete de alerta para paciente: {}", paciEmail);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setTo(paciEmail);
            helper.setSubject(loginDeUsuario);
            String htmlContent = templateEngine.process(TemplateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Lembrete de alerta enviado com sucesso para paciente: {}", paciEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar lembrete para paciente: {} - Erro: {}", paciEmail, e.getMessage());
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }

    public void enviarLembreteDeAlertaParaMedico(String medEmail, String loginDeUsuario, String TemplateName, Map<String, Object> model) throws MessagingException {
        log.info("Enviando lembrete de alerta para medico: {}", medEmail);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        try {
            helper.setTo(medEmail);
            helper.setSubject(loginDeUsuario);
            String htmlContent = templateEngine.process(TemplateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Lembrete de alerta enviado com sucesso para medico: {}", medEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar lembrete para medico: {} - Erro: {}", medEmail, e.getMessage());
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }
}
