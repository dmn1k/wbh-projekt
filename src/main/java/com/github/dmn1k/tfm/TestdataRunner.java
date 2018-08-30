package com.github.dmn1k.tfm;

import com.github.dmn1k.tfm.inserate.Inserat;
import com.github.dmn1k.tfm.inserate.InserateRepository;
import com.github.dmn1k.tfm.inserate.StoriesRepository;
import com.github.dmn1k.tfm.inserate.Story;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TestdataRunner implements ApplicationRunner {
    private final InserateRepository inserateRepository;
    private final StoriesRepository storiesRepository;

    @Override
    public void run(ApplicationArguments args) {
        inserateRepository.save(Inserat.builder()
            .titel("Mein tolles Inserat")
            .beschreibung("bla blubb")
            .build());

        inserateRepository.save(Inserat.builder()
            .titel("Inserat 42")
            .beschreibung("blubdiblubb")
            .build());

        storiesRepository.save(new Story(null, "Story 1", "Blablubb"));
    }
}
