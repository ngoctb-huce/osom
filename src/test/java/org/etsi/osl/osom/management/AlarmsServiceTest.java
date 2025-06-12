import org.etsi.osl.tmf.am642.model.AlarmStateType;
import org.etsi.osl.tmf.am642.model.AlarmUpdate;
import org.etsi.osl.tmf.am642.model.Comment;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

package org.etsi.osl.osom.management;




class AlarmsServiceTest {

    private AlarmsService alarmsService;
    private ProducerTemplate templateMock;
    private CamelContext camelContextMock;

    @BeforeEach
    void setUp() {
        alarmsService = new AlarmsService();
        templateMock = mock(ProducerTemplate.class);
        camelContextMock = mock(CamelContext.class);

        ReflectionTestUtils.setField(alarmsService, "template", templateMock);
        ReflectionTestUtils.setField(alarmsService, "contxt", camelContextMock);
        ReflectionTestUtils.setField(alarmsService, "compname", "testComponent");
        ReflectionTestUtils.setField(alarmsService, "ALARMS_ADD_ALARM", "direct:addAlarm");
        ReflectionTestUtils.setField(alarmsService, "ALARMS_UPDATE_ALARM", "direct:updateAlarm");
    }

    @Test
    void testPatchAlarmClear_ClearedTrue() throws Exception {
        // Arrange
        String alarmId = "alarm123";
        String textNote = " - resolved";
        // Mock updateAlarm to return a string
        AlarmsService spyService = Mockito.spy(alarmsService);
        doReturn("updated").when(spyService).updateAlarm(any(AlarmUpdate.class), eq(alarmId));

        // Act
        spyService.patchAlarmClear(alarmId, textNote, true);

        // Assert
        ArgumentCaptor<AlarmUpdate> captor = ArgumentCaptor.forClass(AlarmUpdate.class);
        verify(spyService).updateAlarm(captor.capture(), eq(alarmId));
        AlarmUpdate update = captor.getValue();

        assertEquals("testComponent", update.getClearSystemId());
        assertEquals(AlarmStateType.cleared.name(), update.getState());
        assertNotNull(update.getComment());
        assertFalse(update.getComment().isEmpty());
        Comment comment = update.getComment().get(0);
        assertTrue(comment.getComment().contains("Alarm cleared by applying actions."));
        assertTrue(comment.getComment().contains(textNote));
        assertEquals("testComponent", comment.getSystemId());
        assertNotNull(comment.getTime());
    }

    @Test
    void testPatchAlarmClear_ClearedFalse() throws Exception {
        // Arrange
        String alarmId = "alarm456";
        String textNote = " - not cleared";
        AlarmsService spyService = Mockito.spy(alarmsService);
        doReturn("updated").when(spyService).updateAlarm(any(AlarmUpdate.class), eq(alarmId));

        // Act
        spyService.patchAlarmClear(alarmId, textNote, false);

        // Assert
        ArgumentCaptor<AlarmUpdate> captor = ArgumentCaptor.forClass(AlarmUpdate.class);
        verify(spyService).updateAlarm(captor.capture(), eq(alarmId));
        AlarmUpdate update = captor.getValue();

        assertNull(update.getClearSystemId());
        assertNull(update.getState());
        assertNotNull(update.getComment());
        assertFalse(update.getComment().isEmpty());
        Comment comment = update.getComment().get(0);
        assertTrue(comment.getComment().contains("Alarm updated, not cleared."));
        assertTrue(comment.getComment().contains(textNote));
        assertEquals("testComponent", comment.getSystemId());
        assertNotNull(comment.getTime());
    }

    @Test
    void testPatchAlarmClear_UpdateAlarmThrowsIOException() throws Exception {
        // Arrange
        String alarmId = "alarm789";
        String textNote = " - error";
        AlarmsService spyService = Mockito.spy(alarmsService);
        doThrow(new IOException("fail")).when(spyService).updateAlarm(any(AlarmUpdate.class), eq(alarmId));

        // Act & Assert: Should not throw, just log error
        assertDoesNotThrow(() -> spyService.patchAlarmClear(alarmId, textNote, true));
        verify(spyService).updateAlarm(any(AlarmUpdate.class), eq(alarmId));
    }
}