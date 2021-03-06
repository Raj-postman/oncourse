package ish.util

import ish.messaging.ICourse
import ish.messaging.ICourseClass
import ish.messaging.ICourseModule
import ish.messaging.IModule
import org.apache.cayenne.ObjectContext
import org.junit.Test
import org.mockito.Matchers
import org.mockito.Mockito
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class CourseUtilTest {

    @Test
    void testAddModule() throws Exception {
        IModule module = Mockito.mock(IModule.class)
        when(module.getNationalCode()).thenReturn("nationalCode")

        ICourseModule courseModule = Mockito.mock(ICourseModule.class)

        ObjectContext context = Mockito.mock(ObjectContext.class)
        when(context.localObject(module)).thenReturn(module)
        when(context.newObject(Matchers.any())).thenReturn(courseModule)

        ICourseClass courseClass = Mockito.mock(ICourseClass.class)
        List courseClasses = new ArrayList<>()
        courseClasses.add(courseClass)


        ICourse course = Mockito.mock(ICourse.class)
        when(course.getContext()).thenReturn(context)
        when(course.getModules()).thenReturn(Collections.emptyList())
        when(course.getCourseClasses()).thenReturn(courseClasses)
        when(course.isModifiedRecord()).thenReturn(false)

        CourseUtil.addModule(course, module, ICourseModule.class)

        verify(courseModule, Mockito.times(1)).setCourse(course)
        verify(courseModule, Mockito.times(1)).setCourse(course)
        verify(courseClass, Mockito.times(1)).addModuleToAllSessions(module)
        verify(course, Mockito.times(1)).setModifiedOn(Matchers.any())
    }
}