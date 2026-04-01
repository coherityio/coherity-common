package io.coherity.shared.common.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.JSONConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CascadingConfigurationFactory
{
	public static CompositeConfiguration createCascadingConfiguration(List<Configuration> configurations)
	{
		CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
		
		compositeConfiguration.addConfiguration(new SystemConfiguration());
		
		//convert environment var names to property names
		compositeConfiguration.addConfiguration(CascadingConfigurationFactory.toStandardPropertyKeyConfiguration(new EnvironmentConfiguration()));
		
		if(configurations != null)
		{
			for(Configuration configuration : configurations)
			{
				compositeConfiguration.addConfiguration(configuration);
			}
		}
		
		return compositeConfiguration;
	}
	
	
	public static CompositeConfiguration createCascadingConfiguration(String[] configurationFileNames)
	{
		List<Configuration> configurationList = new ArrayList<>();
		if(configurationFileNames != null)
		{
			for(String configurationFileName : configurationFileNames)
			{
				if(StringUtils.isBlank(configurationFileName))
				{
					continue;
				}
				try
				{
					Parameters params = new Parameters();
					FileBasedConfigurationBuilder<FileBasedConfiguration> builder;
					String lowerName = configurationFileName.toLowerCase();
					if(lowerName.endsWith(".yml") || lowerName.endsWith(".yaml"))
					{
						builder =
								new FileBasedConfigurationBuilder<FileBasedConfiguration>(YAMLConfiguration.class)
								.configure(params.properties().setFileName(configurationFileName));
					}
					else if(lowerName.endsWith(".json"))
					{
						builder =
								new FileBasedConfigurationBuilder<FileBasedConfiguration>(JSONConfiguration.class)
								.configure(params.properties().setFileName(configurationFileName));
					}
					else
					{
						builder =
								new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
								.configure(params.properties().setFileName(configurationFileName));
					}
					configurationList.add(builder.getConfiguration());
				}
				catch(ConfigurationException ce)
				{
					log.warn("could not load: " + configurationFileName, ce);
				}
			}
		}
		return CascadingConfigurationFactory.createCascadingConfiguration(configurationList);
	}
	
	
	private static String toPropertyName(String environmentVariable)
	{
		if(StringUtils.isNotEmpty(environmentVariable))
		{
			// Normalize to lowercase dot-separated keys; treat '-' and '_' as hierarchy separators.
			return environmentVariable
					.replace("-", ".")
					.replace("_", ".")
					.toLowerCase();
		}
		return "";
	}
	
	private static Configuration toStandardPropertyKeyConfiguration(Configuration configuration)
	{
		//Convert all configuration keys to lowercase separated by "."
		BaseConfiguration normalizedConfiguration = new BaseConfiguration();
		if(configuration != null)
		{
			Iterator<String> keyIterator = configuration.getKeys();
			String environmentKey = null;
			while(keyIterator.hasNext())
			{
				environmentKey = keyIterator.next();
				normalizedConfiguration.setProperty(CascadingConfigurationFactory.toPropertyName(environmentKey), configuration.getString(environmentKey));
			}
		}
		return normalizedConfiguration;
	}
	
}