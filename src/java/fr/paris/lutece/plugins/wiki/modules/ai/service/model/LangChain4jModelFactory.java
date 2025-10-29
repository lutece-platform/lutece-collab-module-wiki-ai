/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.wiki.modules.ai.service.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;

import fr.paris.lutece.portal.service.util.AppLogService;

/**
 * Factory for creating LangChain4j model instances using the builder pattern. This factory dynamically creates model instances by invoking builder methods and
 * setting properties via reflection.
 */
public class LangChain4jModelFactory implements FactoryBean<Object>
{
    private static final String METHOD_BUILDER = "builder";
    private static final String METHOD_BUILD = "build";
    private static final String ERROR_SETTING_PROPERTY = "Error setting property: ";
    private static final String ERROR_ON_BUILDER = " on builder: ";
    private static final String ERROR_MODEL_CLASS_NOT_FOUND = "Model class not found: ";

    private String _modelClass;
    private Map<String, Object> _properties = new HashMap<>( );

    /**
     * Sets the fully qualified class name of the model to be created.
     *
     * @param modelClass
     *            the model class name
     */
    public void setModelClass( String modelClass )
    {
        _modelClass = modelClass;
    }

    /**
     * Sets the properties to be applied to the model builder.
     *
     * @param properties
     *            the properties map
     */
    public void setProperties( Map<String, Object> properties )
    {
        _properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getObject( ) throws Exception
    {
        Class<?> modelClass = Class.forName( _modelClass );
        Method builderMethod = modelClass.getMethod( METHOD_BUILDER );
        Object builder = builderMethod.invoke( null );

        for ( Map.Entry<String, Object> entry : _properties.entrySet( ) )
        {
            String propertyName = entry.getKey( );
            Object propertyValue = entry.getValue( );

            try
            {
                Method setterMethod = findSetterMethod( builder.getClass( ), propertyName );
                if ( setterMethod != null )
                {
                    Object convertedValue = convertValue( propertyValue, setterMethod.getParameterTypes( ) [0] );
                    builder = setterMethod.invoke( builder, convertedValue );
                }
            }
            catch( IllegalAccessException | InvocationTargetException e )
            {
                AppLogService.error( ERROR_SETTING_PROPERTY + propertyName + ERROR_ON_BUILDER + builder.getClass( ).getName( ), e );
            }
        }

        Method buildMethod = builder.getClass( ).getMethod( METHOD_BUILD );
        return buildMethod.invoke( builder );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getObjectType( )
    {
        if ( _modelClass != null )
        {
            try
            {
                return Class.forName( _modelClass );
            }
            catch( ClassNotFoundException e )
            {
                AppLogService.error( ERROR_MODEL_CLASS_NOT_FOUND + _modelClass, e );
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton( )
    {
        return true;
    }

    /**
     * Finds a setter method on the builder class that matches the property name.
     *
     * @param builderClass
     *            the builder class
     * @param propertyName
     *            the property name
     * @return the matching setter method, or null if not found
     */
    private Method findSetterMethod( Class<?> builderClass, String propertyName )
    {
        Method [ ] methods = builderClass.getMethods( );
        for ( Method method : methods )
        {
            if ( method.getName( ).equals( propertyName ) && method.getParameterCount( ) == 1 )
            {
                return method;
            }
        }
        return null;
    }

    /**
     * Converts a value to the target type.
     *
     * @param value
     *            the value to convert
     * @param targetType
     *            the target type
     * @return the converted value
     */
    private Object convertValue( Object value, Class<?> targetType )
    {
        if ( value == null || targetType.isInstance( value ) )
        {
            return value;
        }

        String strValue = value.toString( );

        if ( targetType == Double.class || targetType == double.class )
        {
            return Double.valueOf( strValue );
        }
        else if ( targetType == Integer.class || targetType == int.class )
        {
            return Integer.valueOf( strValue );
        }
        else if ( targetType == Long.class || targetType == long.class )
        {
            return Long.valueOf( strValue );
        }
        else if ( targetType == Float.class || targetType == float.class )
        {
            return Float.valueOf( strValue );
        }
        else if ( targetType == Boolean.class || targetType == boolean.class )
        {
            return Boolean.valueOf( strValue );
        }

        return value;
    }
}
