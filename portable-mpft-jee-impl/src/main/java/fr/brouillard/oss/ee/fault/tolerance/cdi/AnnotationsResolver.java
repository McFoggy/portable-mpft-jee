/**
 * Copyright © 2017 Matthieu Brouillard [http://oss.brouillard.fr/portable-mpft-jee] (matthieu@brouillard.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.brouillard.oss.ee.fault.tolerance.cdi;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.enterprise.inject.Vetoed;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

public class AnnotationsResolver {
    public <E extends Member & AnnotatedElement> Of<Retry> retry(Class<?> bean, E element) {
        return resolverOf(bean, element, Retry.class);
    }
    public <E extends Member & AnnotatedElement> Of<Timeout> timeout(Class<?> bean, E element) {
        return resolverOf(bean, element, Timeout.class);
    }
    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> resolverOf(Class<?> bean, E element, Class<T> ftAnnotation) {
        if (element.isAnnotationPresent(ftAnnotation))
            return elementResolverOf(element, ftAnnotation);
        else
            return beanResolverOf(element, ftAnnotation, bean);
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> elementResolverOf(E element, Class<T> tfAnnotation) {
        T annotation = element.getAnnotation(tfAnnotation);
        String name = name(element, tfAnnotation);
        return new With<>(annotation, name);
    }

//    private <E extends Member & AnnotatedElement> String metricName(E element, Class<? extends Annotation> type, String name, boolean absolute) {
//        String metric = name.isEmpty() ? defaultName(element, type) : metricName.of(name);
//        return absolute ? metric : MetricRegistry.name(element.getDeclaringClass(), metric);
//    }
//
//    private <E extends Member & AnnotatedElement> String metricName(Class<?> bean, E element, Class<? extends Annotation> type, String name, boolean absolute) {
//        String metric = name.isEmpty() ? bean.getSimpleName() : metricName.of(name);
//        return absolute ? MetricRegistry.name(metric, defaultName(element, type)) : MetricRegistry.name(bean.getPackage().getName(), metric, defaultName(element, type));
//    }

    private <E extends Member & AnnotatedElement> String defaultName(E element, Class<? extends Annotation> type) {
        return memberName(element);
    }

    // While the Member Javadoc states that the getName method should returns
    // the simple name of the underlying member or constructor, the FQN is returned
    // for constructors. See JDK-6294399:
    // http://bugs.java.com/view_bug.do?bug_id=6294399
    private String memberName(Member member) {
        if (member instanceof Constructor)
            return member.getDeclaringClass().getSimpleName();
        else
            return member.getName();
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> Of<T> beanResolverOf(E element, Class<T> ftAnnotation, Class<?> bean) {
        if (bean.isAnnotationPresent(ftAnnotation)) {
            T annotation = bean.getAnnotation(ftAnnotation);
            String name = name(bean, element, ftAnnotation);
            return new With<>(annotation, name);
        } else if (bean.getSuperclass() != null) {
            return beanResolverOf(element, ftAnnotation, bean.getSuperclass());
        }
        return new Without<T>();
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> String name(Class<?> bean, E element, Class<T> ftAnnotation) {
        return "";
    }

    private <E extends Member & AnnotatedElement, T extends Annotation> String name(E element, Class<T> ftAnnotation) {
        return "";
    }

    public interface Of<T extends Annotation> {
        boolean isPresent();

        String name();

        T annotation();
    }

    private static final class With<T extends Annotation> implements Of<T> {

        private final T annotation;

        private final String name;

        private With(T annotation, String name) {
            this.annotation = annotation;
            this.name = name;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public T annotation() {
            return annotation;
        }
    }

    @Vetoed
    private static final class Without<T extends Annotation> implements Of<T> {

        private Without() {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String name() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T annotation() {
            throw new UnsupportedOperationException();
        }
    }
}