package com.javarush.jira.profile.internal.web;

import com.javarush.jira.AbstractControllerTest;
import com.javarush.jira.profile.ProfileTo;
import com.javarush.jira.profile.internal.ProfileMapper;
import com.javarush.jira.profile.internal.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.javarush.jira.profile.internal.web.ProfileRestController.REST_URL;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileRestControllerTest extends AbstractControllerTest {

    @Autowired
    ProfileMapper profileMapper;
    @Autowired
    private ProfileRepository repository;

    @Test
    void get() throws Exception {
        perform(MockMvcRequestBuilders.get(ProfileRestController.REST_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void updateProfile() throws Exception {
        ProfileTo profileTo = ProfileTestData.getUpdatedTo();
        perform(MockMvcRequestBuilders.put(REST_URL + "/" + profileTo.getId()))
        .andExpect(status().isOk());
    }

}