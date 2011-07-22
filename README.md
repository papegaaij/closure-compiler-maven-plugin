## About
Look at the original project https://github.com/gli/closure-compiler-maven-plugin

There is no maven repo for this plugin, so you need to download and build it by yourself.

## Configuration

You can configure plugin with properties like this:

    <properties>
        <closure.source>src/main/javascript</closure.source>
        <closure.externs>src/main/externs</closure.externs>
        <closure.outputFile>compiled.js</closure.outputFile>
        <closure.outputDir>${project.build.directory}/${project.build.finalName}</closure.outputDir>
        <closure.output>${closure.outputDir}/${closure.outputFile}</closure.output>
    </properties>
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>com.github.urmuzov</groupId>
                <artifactId>closure-compiler-maven-plugin</artifactId>
                <version>0.1.1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <loggingLevel>WARNING</loggingLevel>
                    <sourceDirectory>${closure.source}</sourceDirectory>
                    <externsSourceDirectory>${closure.externs}</externsSourceDirectory>
                    <outputFile>${closure.output}</outputFile>
                    <compilationLevel>ADVANCED_OPTIMIZATIONS</compilationLevel>
                    <warningLevel>VERBOSE</warningLevel>
                    <manageClosureDependencies>true</manageClosureDependencies>
                    <generateExports>true</generateExports>
                    <addDefaultExterns>true</addDefaultExterns>
                    <logExternFiles>true</logExternFiles>
                    <logSourceFiles>true</logSourceFiles>
                    <stopOnErrors>true</stopOnErrors>
                    <stopOnWarnings>true</stopOnWarnings>
                </configuration>
            </plugin>
            ...
        </plugins>
    </build>
