package com.yas.media;

import com.yas.media.model.Media;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaTest {
    @Test
    void testMediaBasic() {
        Media media = new Media();
        media.setId(1L);
        media.setFileName("image.png");

        assertEquals(1L, media.getId());
        assertEquals("image.png", media.getFileName());
    }
}