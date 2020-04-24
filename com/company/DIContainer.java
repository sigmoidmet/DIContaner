package com.company;

import com.company.annotations.Bean;
import com.company.annotations.Prototype;
import com.company.exceptions.AmbiguousBeanException;
import com.company.exceptions.BadConfigException;
import com.company.exceptions.NoSuchBeanArgumentsException;
import com.company.exceptions.NotUniqueBeanNameException;
import jdk.jshell.spi.ExecutionControl;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class DIContainer {
    final Map<String, Object> beans = new HashMap<>();
    final Map<String, Method> prototypes = new HashMap<>();
    final List<Method> factoryMethods = new ArrayList<>();

    public DIContainer(Class<?>... classes) throws Exception {
        loadConfigs(classes);
        refresh();
    }

    public void loadConfigs(Class<?>... configs){
        for (Class config : configs)
            factoryMethods.addAll(getFactoryMethodsInParameters(config));
        factoryMethods.sort(Comparator.comparingInt(Method::getParameterCount));
    }

    public void refresh() throws Exception {
        List<Method> methodsWithoutCreatedArguments = new ArrayList<>();
        boolean anyCreated = true;
        while (anyCreated) {
            anyCreated = false;
            for (Method method : factoryMethods) {
                if (argumentBeansCreated(method)) {
                    createObjectByMethod(method);
                    anyCreated = true;
                }
                else
                    methodsWithoutCreatedArguments.add(method);
            }
            factoryMethods.clear();
            factoryMethods.addAll(methodsWithoutCreatedArguments);
            methodsWithoutCreatedArguments.clear();
        }

        if (!factoryMethods.isEmpty())
            throw new NoSuchBeanArgumentsException("There are no parameters for one of the beans in config beans.");
    }

    private List<Method> getFactoryMethodsInParameters(Class<?> config) {
        return Arrays
                .stream(config.getDeclaredMethods())
                .filter((m) -> m.isAnnotationPresent(Bean.class))
                .collect(Collectors.toList());
    }

    private boolean argumentBeansCreated(Method method) throws Exception {
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (!isBeanWithTypeExists(parameter.getType(), parameter))
                return false;
        }
        return true;
    }

    private boolean isBeanWithTypeExists(Class<?> type, Parameter parameter) throws Exception {
        boolean beansForArrayExist = false;
        if (type.isArray())
            beansForArrayExist = isBeanWithTypeExists(type.getComponentType(), parameter);

        boolean beansForCollectionExist = false;
        if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pType = (ParameterizedType) parameter.getParameterizedType();
            Type parameterType = pType.getActualTypeArguments()[0];
            beansForCollectionExist = isBeanWithTypeExists(Class.forName(parameterType.getTypeName()), parameter);
        }

        boolean isInBeans = beans.values().stream().anyMatch(type::isInstance);
        boolean isInPrototypes =  prototypes.values().stream().anyMatch((method) -> method.getReturnType().equals(type));

        return beansForCollectionExist || beansForArrayExist || isInBeans || isInPrototypes;
    }

    private void createObjectByMethod(Method method) throws Exception {
        String name = buildNameForObject(method);

        if (beans.containsKey(name))
            throw new NotUniqueBeanNameException("Bean with name " + name + " is already exists.");

        Object obj = createObjectForMethodIfNecessary(method);
        Object[] args = findArgumentsForMethod(method);

        if (method.isAnnotationPresent(Prototype.class))
            prototypes.put(name, method);
        else
            beans.put(name, method.invoke(obj,  args));
    }


    private String buildNameForObject(Method method) {
        String name = method.getAnnotation(Bean.class).value();
        return name.isEmpty() ? buildNameForObjectByMethodName(method) : name;
    }


    private String buildNameForObjectByMethodName(Method method) {
        String name = method.getName();
        long count = beans.keySet().stream().filter((key) -> key.startsWith(name + "#")).count();
        return name + "#" + count;
    }

    public <T> T getBean(String key) throws Exception {
        if (beans.containsKey(key))
            return (T) beans.get((key));
        else if (prototypes.containsKey(key)) {
            Method prototype = prototypes.get(key);
            Object obj = createObjectForMethodIfNecessary(prototype);
            Object[] args = findArgumentsForMethod(prototype);
            return (T) prototype.invoke(obj, args);
        }
        else
            throw new IllegalArgumentException("Key " + key + " doesn't exists.");
    }

    private Object[] findArgumentsForMethod(Method method) throws Exception {

        Parameter[] parameters = method.getParameters();

        if (parameters.length == 0)
            return null;

        List<Object> args = new ArrayList<>();

        for (Parameter parameter : parameters)
            args.add(createParameterByType(parameter, method));
        return args.toArray();
    }

    private Object createParameterByType(Parameter parameter, Method method) throws Exception {
        Object bean = tryFindBeanByParameterType(parameter.getType(), method);

        if (bean == null) {
            bean  = tryCreateCompositeBean(parameter, method);
            if (bean == null)
                throw new NoSuchBeanArgumentsException("There are not parameters in another beans in one of the config beans with type " + parameter.getType().toString());
        }
        return bean;
    }

    private <T> T tryFindBeanByParameterType(Class<T> type, Method method) throws Exception {
        List<T> objects = getBeansByType(type);
        objects.addAll(getCalledPrototypesByType(type, method));
        if (objects.size() > 1)
            throw new AmbiguousBeanException("There are more than one possible beans for one of the arguments with type " + type.toString());

        return (objects.size() == 0) ? null : objects.get(0);
    }

    private Object tryCreateCompositeBean(Parameter parameter, Method method) throws Exception {
        if (parameter.getType().isArray())
            return createArrayFromBeansByType(parameter.getType().getComponentType(), method);
        else if (Collection.class.isAssignableFrom(parameter.getType()))
            return createCollectionFromBeansByType(parameter, method);
        return null;
    }

    private <T> T[] createArrayFromBeansByType(Class<T> componentType, Method method) throws Exception {
        List<T>  objects = createListFromBeansByType(componentType, method);
        T[] array = (T[]) Array.newInstance(componentType, objects.size());
        for (int i = 0; i < array.length; ++i)
            array[i] = objects.get(i);
        return array;
    }

    private Object createCollectionFromBeansByType(Parameter parameter, Method method) throws Exception {
        Collection collection = createCollectionByInterface(parameter.getType());
        ParameterizedType pType = (ParameterizedType) parameter.getParameterizedType();
        Type parameterType = pType.getActualTypeArguments()[0];
        collection.addAll(createListFromBeansByType(Class.forName(parameterType.getTypeName()), method));

        return collection;

    }

    private Collection createCollectionByInterface(Class<?> type) throws Exception {
        if (List.class.isAssignableFrom(type))
            return new ArrayList();
        if (Queue.class.isAssignableFrom(type))
            return new LinkedList();
        if (Set.class.isAssignableFrom(type))
            return new HashSet();
        throw new ExecutionControl.NotImplementedException("This type isn't supported.");
    }

    private <T> List<T> createListFromBeansByType(Class<T> type, Method method) throws Exception {
        List<T> objects = new ArrayList<>();
        objects.addAll(getBeansByType(type));
        objects.addAll(getCalledPrototypesByType(type, method));
        return objects;
    }

    private <T> List<T> getBeansByType(Class<T> type) {
        return beans.values().stream().filter(type::isInstance).map((b) -> (T) b).collect(Collectors.toList());
    }

    private <T> List<T> getCalledPrototypesByType(Class<T> type, Method method) throws Exception {
        List<Method> methods = prototypes.values().stream().filter((p) -> type.equals(p.getReturnType()) && !method.equals(p)).collect(Collectors.toList());
        List<T> result = new ArrayList<>();
        for (Method prototype : methods) {
            Object obj = createObjectForMethodIfNecessary(prototype);
            Object[] args = findArgumentsForMethod(prototype);
            T val = (T) prototype.invoke(obj, args);
            result.add(val);
        }
        return result;
    }


    private Object createObjectForMethodIfNecessary(Method method) throws Exception {
        if (Modifier.isStatic(method.getModifiers()))
            return null;
        else
            try {
                return method.getDeclaringClass().getConstructor().newInstance();
            }
            catch (NoSuchMethodException e) {
                throw new BadConfigException("Nonstatic method requires an object, but there is no default constructor for config with type " + method.getDeclaringClass().toString(), e);
            }
    }

}
