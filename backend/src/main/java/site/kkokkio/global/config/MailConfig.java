package site.kkokkio.global.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

	@Value("${spring.mail.host}")
	private String host;

	@Value("${spring.mail.port}")
	private int port;

	@Value("${spring.mail.username}")
	private String username;

	@Value("${spring.mail.password}")
	private String password;

	@Value("${spring.mail.properties.mail.smtp.auth}")
	private boolean auth;

	@Value("${spring.mail.properties.mail.smtp.starttls.enable}")
	private boolean starttlsEnable;

	@Value("${spring.mail.properties.mail.smtp.starttls.required}")
	private boolean starttlsRequired;

	@Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
	private int connectionTimeout;

	@Value("${spring.mail.properties.mail.smtp.timeout}")
	private int timeout;

	@Value("${spring.mail.properties.mail.smtp.writetimeout}")
	private int writeTimeout;

	// JavaMailSender 설정
	@Bean
	public JavaMailSender getJavaMailSender() {

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

		// SMTP 서버 기본 정보 설정
		mailSender.setHost(host);
		mailSender.setPort(port);
		mailSender.setUsername(username);
		mailSender.setPassword(password);
		mailSender.setDefaultEncoding("UTF-8");
		mailSender.setJavaMailProperties(getMailProperties()); // 추가 SMTP 프로퍼티 설정

		return mailSender;
	}

	// SMTP 전송 관련 프로퍼티를 Properties 객체로 구성
	private Properties getMailProperties() {

		Properties properties = new Properties();

		properties.put("mail.smtp.auth", auth);
		properties.put("mail.smtp.starttls.enable", starttlsEnable);
		properties.put("mail.smtp.starttls.required", starttlsRequired);
		properties.put("mail.smtp.connectiontimeout", connectionTimeout);
		properties.put("mail.smtp.timeout", timeout);
		properties.put("mail.smtp.writetimeout", writeTimeout);

		return properties;
	}
}
