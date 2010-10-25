## Compiling javascript sources

	<plugin>
		<groupId>glisoft</groupId>
		<artifactId>closure-compiler-maven-plugin</artifactId>
		<version>0.0.1-SNAPSHOT</version>
		<configuration>
			<compilationLevel>SIMPLE_OPTIMIZATIONS</compilationLevel>
		</configuration>
		<executions>
			<execution>
				<goals>
					<goal>compile</goal>
				</goals>
			</execution>
		</executions>
	</plugin>
