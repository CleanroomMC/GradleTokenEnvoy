package com.cleanroommc.tokenenvoy;

import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectCaptureTest {

    @Test
    void realizedValuesAreSafe() {
        assertNull(ProjectCapture.diagnose("1.0.0"));
        assertNull(ProjectCapture.diagnose(12));
        assertNull(ProjectCapture.diagnose(null));
        assertFalse(ProjectCapture.captures(0, "project"));
        assertFalse(ProjectCapture.isUserCallableProvider("1.0.0"));
    }

    @Test
    void reportsExplicitProject() {
        Project project = (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[] { Project.class },
                (proxy, method, args) -> null
        );
        assertTrue(ProjectCapture.captures(0, project));
        assertEquals("captures Project", ProjectCapture.diagnose(project));
    }

}
