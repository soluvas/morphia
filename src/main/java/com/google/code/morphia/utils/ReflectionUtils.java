/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.morphia.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.bson.types.CodeWScope;
import org.bson.types.ObjectId;

import com.google.code.morphia.Key;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.MappingException;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

/**
 * Various reflection utility methods, used mainly in the Mapper.
 * 
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings("unchecked")
public class ReflectionUtils
{

    /**
     * Get an array of all fields declared in the supplied class, and all its
     * superclasses (except java.lang.Object).
     * 
     * @param type
     *            the class for which we want to retrieve the Fields
     * @param returnFinalFields
     *            specifies whether to return final fields
     * @return an array of all declared and inherited fields
     */
    public static Field[] getDeclaredAndInheritedFields(final Class type, final boolean returnFinalFields)
    {
        List<Field> allFields = new ArrayList<Field>();
        allFields.addAll(getValidFields(type.getDeclaredFields(), returnFinalFields));
        Class parent = type.getSuperclass();
        while ((parent != null) && (parent != Object.class))
        {
            allFields.addAll(getValidFields(parent.getDeclaredFields(), returnFinalFields));
            parent = parent.getSuperclass();
        }
        return allFields.toArray(new Field[allFields.size()]);
    }

    /**
     * Get a list of all methods declared in the supplied class, and all its
     * superclasses (except java.lang.Object), recursively.
     * 
     * @param type
     *            the class for which we want to retrieve the Methods
     * @param methods
     *            the list to start from (can be null)
     * @return an array of all declared and inherited fields
     */
    public static List<Method> getDeclaredAndInheritedMethods(final Class type)
    {
        return getDeclaredAndInheritedMethods(type, null);
    }

    protected static List<Method> getDeclaredAndInheritedMethods(final Class type, List<Method> methods)
    {
        if ((type == null) || (type == Object.class))
        {
            return methods;
        }
        if (methods == null)
        {
            methods = new ArrayList<Method>();
        }

        Class parent = type.getSuperclass();
        methods = getDeclaredAndInheritedMethods(parent, methods);

        for (Method m : type.getDeclaredMethods())
        {
            if (!Modifier.isStatic(m.getModifiers()))
            {
                methods.add(m);
            }
        }

        return methods;
    }

    public static List<Field> getValidFields(final Field[] fields, final boolean returnFinalFields)
    {
        List<Field> validFields = new ArrayList<Field>();
        // we ignore static and final fields
        for (Field field : fields)
        {
            if (!Modifier.isStatic(field.getModifiers())
                    && (returnFinalFields || !Modifier.isFinal(field.getModifiers())))
            {
                validFields.add(field);
            }
        }
        return validFields;
    }

    public static boolean implementsAnyInterface(final Class type, final Class... interfaceClasses)
    {
        for (Class iF : interfaceClasses)
        {
            if (implementsInterface(type, iF))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a class implements a specific interface.
     * 
     * @param type
     *            the class we want to check
     * @param interfaceClass
     *            the interface class we want to check against
     * @return true if type implements interfaceClass, else false
     */
    public static boolean implementsInterface(final Class type, final Class interfaceClass)
    {
        return interfaceClass.isAssignableFrom(type);
    }

    /**
     * Check if a class extends a specific class.
     * 
     * @param type
     *            the class we want to check
     * @param superClass
     *            the super class we want to check against
     * @return true if type implements superClass, else false
     */
    public static boolean extendsClass(final Class type, final Class superClass)
    {
        return superClass.isAssignableFrom(type);
    }

    /**
     * Check if the class supplied represents a valid property type.
     * 
     * @param type
     *            the class we want to check
     * @return true if the class represents a valid property type
     */
    public static boolean isPropertyType(final Class type)
    {
        if (type == null)
        {
            return false;
        }

        return (type == String.class) || (type == char.class) || (type == Character.class) || (type == short.class)
                || (type == Short.class) || (type == Integer.class) || (type == int.class) || (type == Long.class)
                || (type == long.class) || (type == Double.class) || (type == double.class) || (type == float.class)
                || (type == Float.class) || (type == Boolean.class) || (type == boolean.class) || (type == Byte.class)
                || (type == byte.class) || (type == Date.class) || (type == Locale.class) || (type == DBRef.class)
                || (type == Pattern.class) || (type == CodeWScope.class) || (type == ObjectId.class) || (type == Key.class) || type.isEnum();
    }

    /**
     * Get the (first) class that parameterizes the Field supplied.
     * 
     * @param field
     *            the field
     * @return the class that parameterizes the field, or null if field is not
     *         parameterized
     */
    public static Class getParameterizedClass(final Field field)
    {
        return getParameterizedClass(field, 0);
    }

    /**
     * Get the class that parameterizes the Field supplied, at the index
     * supplied (field can be parameterized with multiple param classes).
     * 
     * @param field
     *            the field
     * @param index
     *            the index of the parameterizing class
     * @return the class that parameterizes the field, or null if field is not
     *         parameterized
     */
    public static Class getParameterizedClass(final Field field, final int index)
    {
        if (field.getGenericType() instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            if ((ptype.getActualTypeArguments() != null) && (ptype.getActualTypeArguments().length <= index))
            {
                return null;
            }
            Type paramType = ptype.getActualTypeArguments()[index];
            if (paramType instanceof GenericArrayType)
            {
                Class arrayType = (Class) ((GenericArrayType) paramType).getGenericComponentType();
                return Array.newInstance(arrayType, 0).getClass();
            }
            else
            {
                if (paramType instanceof ParameterizedType)
                {
                    ParameterizedType paramPType = (ParameterizedType) paramType;
                    return (Class) paramPType.getRawType();
                }
                else
                {
                    if (paramType instanceof TypeVariable)
                    {
                        // TODO: Figure out what to do... Walk back up the to
                        // the parent class and try to get the variable type
                        // from the T/V/X
                        throw new MappingException("Generic Typed Class not supported:  <"
                                + ((TypeVariable) paramType).getName() + "> = "
                                + ((TypeVariable) paramType).getBounds()[0]);
                    }
                    else
                        if (paramType instanceof Class)
                        {
                            return (Class) paramType;
                        }
                        else
                        {
                            throw new MappingException(
                                    "Unknown type... pretty bad... call for help, wave your hands... yeah!");
                        }
                }
            }
        }
        return null;
    }

    public static Class getTypeArgumentOfParameterizedClass(final Field field, final int index, final int typeIndex)
    {
        if (field.getGenericType() instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            Type paramType = ptype.getActualTypeArguments()[index];
            if (!(paramType instanceof GenericArrayType))
            {
                if (paramType instanceof ParameterizedType)
                {
                    ParameterizedType paramPType = (ParameterizedType) paramType;
                    Type paramParamType = paramPType.getActualTypeArguments()[typeIndex];
                    if (!(paramParamType instanceof ParameterizedType))
                    {
                        return (Class) paramParamType;
                    }
                }
            }
        }
        return null;
    }

    public static Class getParameterizedClass(final Class c)
    {
        return getParameterizedClass(c, 0);
    }

    public static Class getParameterizedClass(final Class c, final int index)
    {
        TypeVariable[] typeVars = c.getTypeParameters();
        if (typeVars.length > 0)
        {
            TypeVariable typeVariable = typeVars[index];
			Type[] bounds = typeVariable.getBounds();
			
			Type type = bounds[0];
			if (type instanceof Class) {
				return (Class) type;// broke for enumset, cause bounds contain
									// type instead of class
			}
			else
				return null;
        }
        else
        {
            return null;
        }
    }

    /**
     * Check if a field is parameterized with a specific class.
     * 
     * @param field
     *            the field
     * @param c
     *            the class to check against
     * @return true if the field is parameterized and c is the class that
     *         parameterizes the field, or is an interface that the
     *         parameterized class implements, else false
     */
    public static boolean isFieldParameterizedWithClass(final Field field, final Class c)
    {
        if (field.getGenericType() instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            for (Type type : ptype.getActualTypeArguments())
            {
                if (type == c)
                {
                    return true;
                }
                if (c.isInterface() && implementsInterface((Class) type, c))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the field supplied is parameterized with a valid JCR property
     * type.
     * 
     * @param field
     *            the field
     * @return true if the field is parameterized with a valid JCR property
     *         type, else false
     */
    public static boolean isFieldParameterizedWithPropertyType(final Field field)
    {
        if (field.getGenericType() instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType) field.getGenericType();
            for (Type type : ptype.getActualTypeArguments())
            {
                if (isPropertyType((Class) type))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the (first) instance of the annotation, on the class (or any
     * superclass, or interfaces implemented).
     */
    public static <T> T getAnnotation(final Class c, final Class<T> ann)
    {
        // TODO isn´t that actually breaking the contract of @Inherited?
        if (c.isAnnotationPresent(ann))
        {
            return (T) c.getAnnotation(ann);
        }
        else
        {
            // need to check all superclasses
            Class parent = c.getSuperclass();
            while ((parent != null) && (parent != Object.class))
            {
                if (parent.isAnnotationPresent(ann))
                {
                    return (T) parent.getAnnotation(ann);
                }

                // ...and interfaces that the superclass implements
                for (Class interfaceClass : parent.getInterfaces())
                {
                    if (interfaceClass.isAnnotationPresent(ann))
                    {
                        return (T) interfaceClass.getAnnotation(ann);
                    }
                }

                parent = parent.getSuperclass();
            }

            // ...and all implemented interfaces
            for (Class interfaceClass : c.getInterfaces())
            {
                if (interfaceClass.isAnnotationPresent(ann))
                {
                    return (T) interfaceClass.getAnnotation(ann);
                }
            }
        }
        // no annotation found, use the defaults
        return null;
    }

    public static Embedded getClassEmbeddedAnnotation(final Class c)
    {
        return getAnnotation(c, Embedded.class);
    }

    public static Entity getClassEntityAnnotation(final Class c)
    {
        return getAnnotation(c, Entity.class);
    }

    private static String stripFilenameExtension(final String filename)
    {
        if (filename.indexOf('.') != -1)
        {
            return filename.substring(0, filename.lastIndexOf('.'));
        }
        else
        {
            return filename;
        }
    }

    public static Set<Class<?>> getFromDirectory(final File directory, final String packageName)
            throws ClassNotFoundException
    {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (directory.exists())
        {
            for (String file : directory.list())
            {
                if (file.endsWith(".class"))
                {
                    String name = packageName + '.' + stripFilenameExtension(file);
                    Class<?> clazz = Class.forName(name);
                    classes.add(clazz);
                }
            }
        }
        return classes;
    }

    public static Set<Class<?>> getFromJARFile(final String jar, final String packageName) throws IOException,
            FileNotFoundException, ClassNotFoundException
    {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        JarInputStream jarFile = new JarInputStream(new FileInputStream(jar));
        JarEntry jarEntry;
        do
        {
            jarEntry = jarFile.getNextJarEntry();
            if (jarEntry != null)
            {
                String className = jarEntry.getName();
                if (className.endsWith(".class"))
                {
                    className = stripFilenameExtension(className);
                    if (className.startsWith(packageName))
                    {
                        classes.add(Class.forName(className.replace('/', '.')));
                    }
                }
            }
        }
        while (jarEntry != null);
        return classes;
    }

    public static Set<Class<?>> getClasses(final String packageName) throws IOException, ClassNotFoundException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return getClasses(loader, packageName);
    }

    public static Set<Class<?>> getClasses(final ClassLoader loader, final String packageName) throws IOException,
            ClassNotFoundException
    {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = loader.getResources(path);
        if (resources != null)
        {
            while (resources.hasMoreElements())
            {
                String filePath = resources.nextElement().getFile();
                // WINDOWS HACK
                if (filePath.indexOf("%20") > 0)
                {
                    filePath = filePath.replaceAll("%20", " ");
                }
                if (filePath != null)
                {
                    if ((filePath.indexOf("!") > 0) & (filePath.indexOf(".jar") > 0))
                    {
                        String jarPath = filePath.substring(0, filePath.indexOf("!")).substring(
                                filePath.indexOf(":") + 1);
                        // WINDOWS HACK
                        if (jarPath.indexOf(":") >= 0)
                        {
                            jarPath = jarPath.substring(1);
                        }
                        classes.addAll(getFromJARFile(jarPath, path));
                    }
                    else
                    {
                        classes.addAll(getFromDirectory(new File(filePath), packageName));
                    }
                }
            }
        }
        return classes;
    }

    public static Object createInstance(final Class type)
    {
        try
        {
            // allows private/protected constructors
            Constructor constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * creates an instance of testType (if it isn't Object.class or null) or
     * fallbackType
     */
    public static Object newInstance(final Constructor tryMe, final Class fallbackType)
    {
        if (tryMe != null)
        {
            tryMe.setAccessible(true);
            try
            {
                return tryMe.newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return createInstance(fallbackType);
    }

    public static Class getClassForName(final String className, final Class defaultClass)
    {
        try
        {
            Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return c;
        }
        catch (ClassNotFoundException ex)
        {
            return defaultClass;
        }
    }

	public static Object createInstance(final Class entityClass, final DBObject dbObject)
    {
        // see if there is a className value
        String className = (String) dbObject.get(Mapper.CLASS_NAME_FIELDNAME);
        Class c = entityClass;
        if (className != null)
        {
            // try to Class.forName(className) as defined in the dbObject first,
            // otherwise return the entityClass
            c = getClassForName(className, entityClass);
        }
        return createInstance(c);
    }

    public static Object newInstance(final Class<?> c, final Class<?> fallbackType)
    {
        return newInstance(getNoArgsConstructor(c), fallbackType);
    }

    public static Constructor getNoArgsConstructor(final Class ctorType)
    {
        try
        {
            Constructor ctor = ctorType.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor;
        }
        catch (NoSuchMethodException e)
        {
            throw new MappingException("No usable constructor for " + ctorType.getName(), e);
        }
    }

    public static boolean isSet(final Class<?> c)
    {
        return Set.class.isAssignableFrom(c);
    }
	
	public static boolean isMap(Class c) {
		return implementsInterface(c, Map.class);
	}
	
	public static boolean isCollection(Class c) {
		return implementsInterface(c, Collection.class);
	}

	public static Object[] convertToArray(final Class type, final Collection values)
	{
		Object exampleArray = Array.newInstance(type, 1);
		Object[] array = ((ArrayList) values).toArray((Object[]) exampleArray);
		return array;
	}

	/** turns the object into an ObjectId if it is/should-be one */
	public static Object asObjectIdMaybe(final Object id)
	{
		try
		{
			if ((id instanceof String) && ObjectId.isValid((String) id))
			{
				return new ObjectId((String) id);
			}
		}
		catch (Exception e)
		{
			// sometimes isValid throws exceptions... bad!
		}
		return id;
	}
}
