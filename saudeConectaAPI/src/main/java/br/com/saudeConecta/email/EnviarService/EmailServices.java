package br.com.saudeConecta.email.EnviarService;

import br.com.saudeConecta.infra.exceptions.EmailServiceException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jdk.jfr.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;


@Service
public class EmailServices {


    @Autowired
    private JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    @Name("Falando de forma mais objetiva esse atributo entra no arquivo aplications.proprites e LÊ tudo que começa com \"spring.mail.username\" e pega as informações de contato  ")
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailServices(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;

    }

    @Name("Método utilizado para enviar  um email simples com um codigo e validação ")
    public void enviarEmail(String destinatario, String assunto, String corpo) throws EmailServiceException {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(assunto);
            message.setText(corpo);


            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailServiceException("Erro ao enviar e-mail");
        }
    }






    @Name("Método utilizado para enviar  um email com uma mensagem instrutiva +  o codigo de validação ")
    public void enviarEmailComPaginaHTML(String to, String subject, String templateName, Map<String, Object> model) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        try {
            helper.setTo(to);
            helper.setSubject(subject);

            //Processar o template Thymeleaf com as variáveis do mapa
            String htmlContent = templateEngine.process(templateName, new Context(Locale.getDefault(), model));
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new MessagingException("Erro ao enviar e-mail ");
        }
    }


}
