# helper usage

## connect to helper
1. Use adb to start helper
    ```shell
    adb shell CLASSPATH=/data/local/tmp/automobile/helper.jar app_process / com.eiyooooo.automobile.helper.AppServer
    ```
2. Use adb forward to connect to helper
    ```shell
    adb forward tcp:14000 tcp:14000
    ```

## send GET request for data
1. /info
    - Get phone info

2. /tasks
    - Get the recent tasks

3. /icon
    - Get the icon of the app
    - option:
        1. 'path'(app's apk path) or 'package'(app package name)

4. /allAppInfo
    - Get all app info
    - option:
        1. 'app_type' (0: all, 1: third-party, 2: system)

5. /appInfos
    - Get the information of the specified app list
    - option:
        1. 'package' (app package name, array)

6. /appDetail
    - Get the detailed information of a single app
    - option:
        1. 'package' (app package name)

7. /taskThumbnail
    - Get the thumbnail of the task
    - option:
        1. 'id'(task id)

8. /appMainActivity
    - Get the main activity by package name
    - option:
        1. 'package'(app package name)

9. /createVirtualDisplay
    - Create virtual display
    - option:
        1. 'width' (display width, if not set, use default display's width)
        2. 'height' (display height, if not set, use default display's height)
        3. 'density' (display density, if not set, use default display's density)

10. /resizeDisplay
    - Resize display
    - option:
        1. 'id' (display id, if not set, use default display)
        2. 'width' (display width, if both 'width' and 'height' not set, not change size)
        3. 'height' (display height, if both 'width' and 'height' not set, not change size)
        4. 'density' (display density, if not set, not change density)

11. /releaseVirtualDisplay
    - Release virtual display
    - option:
        1. 'id' (display id)

12. /openApp
    - Open an app by package name
    - option:
        1. 'package' (app package name)
        2. 'activity' (app activity name, if not set, use main activity)
        3. 'displayId' (display id, if not set, use default display)

13. /stopApp
    - Stop an app by package name
    - option:
        1. 'package' (app package name)

14. /appActivity
    - Get all activities of an app
    - option:
        1. 'package' (app package name)

15. /appPermission
    - Get all permissions of an app
    - option:
        1. 'package' (app package name)

16. /displayInfo
    - Get all display info

17. /runShell
    - Run shell command
    - option:
        1. 'cmd' (shell command)