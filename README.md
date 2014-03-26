# Configuration

Use File -> Settings Server... to configure settings server.

Specify URL of Git repository. Please note — ssh is not yet supported. Please use HTTP/HTTPS. File URL is supported, you will be prompted to init repository if specified path is not exists or repository is not created.
[GitHub](www.github.com) could be used to store settings.

Check "Update repository from remote on start" if you want automatically update your configuration on IDE start. Otherwise you can use Vcs -> Sync Settings...

On first sync you will be prompted to specify login/password. In case of GitHub strongly recommended to not use your login/password — consider to use an [access token](https://help.github.com/articles/creating-an-access-token-for-command-line-use). Leave password empty if you use token instead of login.