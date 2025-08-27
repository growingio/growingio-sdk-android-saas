package com.growingio.android.sdk.utils

import android.content.pm.PackageManager
import spock.lang.Specification

class PermissionUtilTest extends Specification {

    PermissionUtil permissionUtil;
    PackageManager mockPackageManager;
    String mockPackageName;

    def setup(){
        mockPackageManager = Mock(PackageManager)
        mockPackageName = "com.cliff"
        permissionUtil = new PermissionUtil(mockPackageManager,mockPackageName)
    }

    def "normal check and cache"(){
        setup:
        def permissionName1 = "ldk.permission.name"

        when:
        def result = permissionUtil.checkPermission(permissionName1, 1)
        then:
        result
        1 * mockPackageManager.checkPermission(permissionName1, mockPackageName) >> PackageManager.PERMISSION_GRANTED

        when:
        result = permissionUtil.checkPermission(permissionName1,1)
        then:
        result
        0 * mockPackageManager.checkPermission(_, _)

        when:
        def result2 = permissionUtil.checkPermission(permissionName1, 1 << 1)
        then:
        !result2
        1 * mockPackageManager.checkPermission(permissionName1, mockPackageName) >> PackageManager.PERMISSION_DENIED
    }

    def "check permission exception"(){
        setup:
        def permissionName = 'ldk.test.permission'
        when:
        def result = permissionUtil.checkPermission(permissionName, 1 << 2)
        then:
        !result
        1 * mockPackageManager.checkPermission(permissionName, mockPackageName) >> { throw new Throwable("my exception") }

        when:
        result = permissionUtil.checkPermission(permissionName, 1 << 2)
        then:
        !result
        1 * mockPackageManager.checkPermission(permissionName, mockPackageName) >> { throw new Throwable("my exception") }

        when:
        result = permissionUtil.checkPermission(permissionName, 1 << 2)
        then:
        result
        1 * mockPackageManager.checkPermission(permissionName, mockPackageName) >> PackageManager.PERMISSION_GRANTED
    }
}
