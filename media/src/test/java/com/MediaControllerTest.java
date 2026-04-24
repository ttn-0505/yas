// File thêm mới cho unit test
@WebMvcTest(MediaController.class)
class MediaControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @Test
    void getMedia_shouldReturnOk() throws Exception {
        MediaVm mediaVm = new MediaVm(1L, "caption", "file.png", "image/png", "http://url");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(get("/medias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("caption"));
    }
}