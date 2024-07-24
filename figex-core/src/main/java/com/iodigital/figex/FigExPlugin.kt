package com.iodigital.figex


import org.gradle.api.Plugin
import org.gradle.api.Project

class FigExPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register(
            "exportFigma",
            ExportFigmaTask::class.java
        )

        project.extensions.create(
            "figex",
            FigExExtension::class.java,
            project
        )
    }
}
