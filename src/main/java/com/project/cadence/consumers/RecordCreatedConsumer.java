package com.project.cadence.consumers;

import com.project.cadence.dto.Topics;
import com.project.cadence.events.RecordCreatedEvent;
import com.project.cadence.model.Artist;
import com.project.cadence.model.Record;
import com.project.cadence.model.User;
import com.project.cadence.repository.RecordRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RecordCreatedConsumer {
    private final RecordRepository recordRepository;
    private final JavaMailSender mailSender;
    @Value("${frontend.url}")
    private String frontendUrl;

    @KafkaListener(topics = Topics.RECORD_CREATED_TOPIC, groupId = "cadence-group")
    public void notifyFollowersOfNewRelease(RecordCreatedEvent event) {
        Record record = recordRepository.findById(event.getRecordId())
                .orElseThrow();

        String recordTitle = record.getTitle();
        String artistNames = record.getArtists()
                .stream()
                .map(Artist::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        Set<User> followers = new HashSet<>();

        for (Artist artist : record.getArtists()) {
            followers.addAll(artist.getArtistFollowers());
        }

        followers.stream()
                .filter(User::isEmailVerified)
                .forEach(user ->
                        sendReleaseMail(user.getEmail(), recordTitle, artistNames, record.getCoverUrl(), record.getId())
                );
    }

    @Async
    private void sendReleaseMail(String email,
                                 String recordTitle,
                                 String artistNames, String coverUrl, String recordId) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("New Release from " + artistNames);

            String htmlContent = buildReleaseTemplate(recordTitle, artistNames, coverUrl, recordId);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildReleaseTemplate(String recordTitle,
                                        String artistNames,
                                        String coverUrl,
                                        String recordId) {

        return """
                <html>
                <body style="margin:0; padding:0; background:#f2f2f2; font-family:Arial, sans-serif;">
                    <div style="max-width:600px; margin:auto; background:white; padding:20px;">
                
                        <h2 style="color:#1db954; margin-bottom:10px;">
                            🎵 New Release from %s
                        </h2>
                
                        <img src="%s"
                             alt="Record Cover"
                             style="width:100%%; border-radius:10px; margin-bottom:15px;" />
                
                        <h3 style="margin:0;">"%s"</h3>
                
                        <p style="color:#555;">
                            A new record just dropped. Be the first to listen.
                        </p>
                
                        <a href="%s"
                           style="display:inline-block;
                                  margin-top:15px;
                                  padding:12px 24px;
                                  background:#1db954;
                                  color:white;
                                  text-decoration:none;
                                  border-radius:25px;
                                  font-weight:bold;">
                            ▶ Listen Now
                        </a>
                
                        <hr style="margin:30px 0;" />
                
                        <p style="font-size:12px; color:#888;">
                            You’re receiving this because you follow %s on Cadence.
                        </p>
                
                    </div>
                </body>
                </html>
                """.formatted(artistNames, coverUrl, recordTitle, frontendUrl + "records/" + recordId, artistNames);
    }
}
