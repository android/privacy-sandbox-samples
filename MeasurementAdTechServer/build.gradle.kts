/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "2.6.6"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  kotlin("jvm") version "1.6.10"
  kotlin("plugin.spring") version "1.6.10"
  id("com.google.cloud.tools.appengine") version "2.4.2"
}

apply {
  plugin("com.google.cloud.tools.appengine")
}

group = "com.example"
version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks {
  bootJar {
    launchScript()
  }
  jar {
    enabled = false
  }
}

appengine {
  deploy {
    projectId = "project-id" // Use projectId = "GCLOUD_CONFIG" to use project from gcloud config
    version = "1"
  }
  stage {
    setArtifact(buildDir.path + "/libs/" + rootProject.name + "-" + version + ".jar")
  }
}
