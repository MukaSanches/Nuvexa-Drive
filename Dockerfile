FROM nextcloud:33.0.5-apache

COPY .nuvexa/server/nuvexa-entrypoint.sh /usr/local/bin/nuvexa-entrypoint
COPY .nuvexa/server/nuvexa-provision.php /usr/src/nextcloud/nuvexa-provision.php
COPY .nuvexa/server/nuvexa-health.php /usr/src/nextcloud/nuvexa-health.php

RUN chmod 0755 /usr/local/bin/nuvexa-entrypoint \
    && chown www-data:www-data /usr/src/nextcloud/nuvexa-provision.php /usr/src/nextcloud/nuvexa-health.php

ENTRYPOINT ["/usr/local/bin/nuvexa-entrypoint"]
CMD ["apache2-foreground"]
