package com.looksee.journeyExpander;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.mapper.Body;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;

class AuditControllerTest {
    private AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
    }

    @Test
    void receiveMessageReturnsBadRequestWhenBodyIsNull() {
        ResponseEntity<String> response = controller.receiveMessage(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Message data is required", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenDataIsBlank() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        when(body.getMessage().getData()).thenReturn("   ");

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Message data is required", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenPayloadIsNotBase64() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        when(body.getMessage().getData()).thenReturn("%%%not_base64%%%");

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void receiveMessageReturnsBadRequestWhenDecodedPayloadIsInvalidJson() {
        Body body = mock(Body.class, RETURNS_DEEP_STUBS);
        String encoded = Base64.getEncoder().encodeToString("not-json".getBytes(StandardCharsets.UTF_8));
        when(body.getMessage().getData()).thenReturn(encoded);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid message payload", response.getBody());
    }

    @Test
    void shouldBeExpandedReturnsFalseForNullJourney() throws Exception {
        assertFalse(invokeShouldBeExpanded(null));
    }

    @Test
    void shouldBeExpandedReturnsFalseForJourneyWithNullSteps() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(null);

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForJourneyWithEmptySteps() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of());

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseWhenLastStepIsNull() throws Exception {
        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of((Step) null));

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsTrueForLandingStepWithStartPage() throws Exception {
        LandingStep step = mock(LandingStep.class);
        when(step.getStartPage()).thenReturn(mock(PageState.class));

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertTrue(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForLandingStepWithoutStartPage() throws Exception {
        LandingStep step = mock(LandingStep.class);
        when(step.getStartPage()).thenReturn(null);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForSimpleStepWithoutStartPage() throws Exception {
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(null);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseForSimpleStepWithoutEndPage() throws Exception {
        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(mock(PageState.class));
        when(step.getEndPage()).thenReturn(null);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsFalseWhenSimpleStepDoesNotChangePageKey() throws Exception {
        PageState start = mock(PageState.class);
        PageState end = mock(PageState.class);
        when(start.getKey()).thenReturn("page-key");
        when(end.getKey()).thenReturn("page-key");

        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(start);
        when(step.getEndPage()).thenReturn(end);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertFalse(invokeShouldBeExpanded(journey));
    }

    @Test
    void shouldBeExpandedReturnsTrueWhenSimpleStepChangesPageKey() throws Exception {
        PageState start = mock(PageState.class);
        PageState end = mock(PageState.class);
        when(start.getKey()).thenReturn("start");
        when(end.getKey()).thenReturn("end");

        SimpleStep step = mock(SimpleStep.class);
        when(step.getStartPage()).thenReturn(start);
        when(step.getEndPage()).thenReturn(end);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(step));

        assertTrue(invokeShouldBeExpanded(journey));
    }

    @Test
    void existsInJourneyReturnsFalseForNullInputs() throws Exception {
        Journey journey = mock(Journey.class);
        Step step = mock(Step.class);

        assertFalse(invokeExistsInJourney(null, step));
        assertFalse(invokeExistsInJourney(journey, null));
    }

    @Test
    void existsInJourneyReturnsTrueForMatchingLandingStepStartPageKeys() throws Exception {
        PageState page1 = mock(PageState.class);
        PageState page2 = mock(PageState.class);
        when(page1.getKey()).thenReturn("landing-key");
        when(page2.getKey()).thenReturn("landing-key");

        LandingStep existing = mock(LandingStep.class);
        when(existing.getStartPage()).thenReturn(page1);

        LandingStep candidate = mock(LandingStep.class);
        when(candidate.getStartPage()).thenReturn(page2);

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));

        assertTrue(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsTrueForMatchingSimpleSteps() throws Exception {
        PageState start1 = mock(PageState.class);
        PageState start2 = mock(PageState.class);
        when(start1.getUrl()).thenReturn("https://example.com");
        when(start2.getUrl()).thenReturn("https://example.com");

        ElementState element1 = mock(ElementState.class);
        ElementState element2 = mock(ElementState.class);
        when(element1.getKey()).thenReturn("btn-1");
        when(element2.getKey()).thenReturn("btn-1");

        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(start1);
        when(existing.getElementState()).thenReturn(element1);
        when(existing.getAction()).thenReturn(Action.CLICK);
        when(existing.getActionInput()).thenReturn("");

        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(start2);
        when(candidate.getElementState()).thenReturn(element2);
        when(candidate.getAction()).thenReturn(Action.CLICK);
        when(candidate.getActionInput()).thenReturn("");

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));

        assertTrue(invokeExistsInJourney(journey, candidate));
    }

    @Test
    void existsInJourneyReturnsFalseWhenSimpleStepAttributesDiffer() throws Exception {
        PageState start1 = mock(PageState.class);
        PageState start2 = mock(PageState.class);
        when(start1.getUrl()).thenReturn("https://example.com/a");
        when(start2.getUrl()).thenReturn("https://example.com/b");

        ElementState element1 = mock(ElementState.class);
        ElementState element2 = mock(ElementState.class);
        when(element1.getKey()).thenReturn("btn-1");
        when(element2.getKey()).thenReturn("btn-2");

        SimpleStep existing = mock(SimpleStep.class);
        when(existing.getStartPage()).thenReturn(start1);
        when(existing.getElementState()).thenReturn(element1);
        when(existing.getAction()).thenReturn(Action.CLICK);
        when(existing.getActionInput()).thenReturn("");

        SimpleStep candidate = mock(SimpleStep.class);
        when(candidate.getStartPage()).thenReturn(start2);
        when(candidate.getElementState()).thenReturn(element2);
        when(candidate.getAction()).thenReturn(Action.CLICK);
        when(candidate.getActionInput()).thenReturn("different");

        Journey journey = mock(Journey.class);
        when(journey.getSteps()).thenReturn(List.of(existing));

        assertFalse(invokeExistsInJourney(journey, candidate));
    }

    private boolean invokeShouldBeExpanded(Journey journey) throws Exception {
        Method method = AuditController.class.getDeclaredMethod("shouldBeExpanded", Journey.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, journey);
    }

    private boolean invokeExistsInJourney(Journey journey, Step step) throws Exception {
        Method method = AuditController.class.getDeclaredMethod("existsInJourney", Journey.class, Step.class);
        method.setAccessible(true);
        return (boolean) method.invoke(controller, journey, step);
    }
}
