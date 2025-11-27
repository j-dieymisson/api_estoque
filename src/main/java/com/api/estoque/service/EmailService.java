package com.api.estoque.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    // Lê o email configurado no application.properties para usar como remetente
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Envia um email formatado em HTML de forma assíncrona.
     * * @param destinatario O email de destino (ex: joao@empresa.com)
     * @param assunto O título do email
     * @param nomeTemplate O nome do ficheiro HTML na pasta templates (sem .html)
     * @param contexto As variáveis para preencher no template (ex: nome, id, status)
     */
    @Async // <--- A MÁGICA: Isto faz o envio correr numa thread separada
    public void enviarEmailHtml(String destinatario, String assunto, String nomeTemplate, Context contexto) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // 'true' indica que é multipart (permite anexos e HTML)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 1. Processa o Template HTML com as variáveis
            String htmlContent = templateEngine.process(nomeTemplate, contexto);

            // 2. Configura o Email
            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(htmlContent, true); // true = o conteúdo é HTML

            // 3. Envia
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            // Logamos o erro, mas NÃO lançamos exceção para não quebrar a resposta ao utilizador
            System.err.println("❌ Falha ao enviar email para " + destinatario + ": " + e.getMessage());
        }
    }
}