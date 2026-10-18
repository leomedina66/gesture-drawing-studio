' Launches the app without a console window, from this script's own folder.
Set fso = CreateObject("Scripting.FileSystemObject")
Set WshShell = CreateObject("WScript.Shell")
appDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.Run "javaw -jar """ & appDir & "\target\gesture-drawing-studio.jar""", 0, False
