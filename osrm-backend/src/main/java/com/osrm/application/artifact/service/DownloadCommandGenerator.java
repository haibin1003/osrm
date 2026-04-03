package com.osrm.application.artifact.service;

import com.osrm.domain.software.entity.SoftwareType;

/**
 * 下载命令生成器
 * 根据软件类型生成对应的下载/拉取命令
 */
public class DownloadCommandGenerator {

    /**
     * 生成下载命令
     */
    public static String generate(SoftwareType type, String packageName, String version, String fileName) {
        return switch (type) {
            case DOCKER_IMAGE -> generateDockerCommand(packageName, version);
            case HELM_CHART -> generateHelmCommand(packageName, version);
            case MAVEN -> generateMavenCommand(packageName, version);
            case NPM -> generateNpmCommand(packageName, version);
            case PYPI -> generatePypiCommand(packageName, version);
            case GENERIC -> generateGenericCommand(packageName, version);
        };
    }

    /**
     * Docker 镜像拉取命令
     */
    public static String generateDockerCommand(String packageName, String version) {
        return "docker pull <registry>/" + packageName + ":" + version;
    }

    /**
     * Helm Chart 拉取命令
     */
    public static String generateHelmCommand(String packageName, String version) {
        return "helm pull <registry>/" + packageName + " --version " + version;
    }

    /**
     * Maven 依赖坐标
     */
    public static String generateMavenCommand(String packageName, String version) {
        return "<dependency>\n" +
               "  <groupId>com.example</groupId>\n" +
               "  <artifactId>" + packageName + "</artifactId>\n" +
               "  <version>" + version + "</version>\n" +
               "</dependency>";
    }

    /**
     * NPM 安装命令
     */
    public static String generateNpmCommand(String packageName, String version) {
        return "npm install " + packageName + "@" + version;
    }

    /**
     * PyPI 安装命令
     */
    public static String generatePypiCommand(String packageName, String version) {
        return "pip install " + packageName + "==" + version;
    }

    /**
     * 通用文件下载链接
     */
    public static String generateGenericCommand(String packageName, String version) {
        return "wget https://<host>/api/v1/artifacts/download/" + packageName + "/" + version;
    }
}
