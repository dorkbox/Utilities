#!/bin/sh
# This installs our cert into firefox, thunderbird, and chrome for linux
# usage: incert.sh cert_name
#


# install for global OS purposes:
#    Given a CA ceritificate file 'foo.crt', follow these steps to install it on Ubuntu:
#
#        Create a directory for extra CA certificates in /usr/share/ca-certificates
#
#        sudo mkdir /usr/share/ca-certificates/extra
#        Copy the '.crt' file to the directory
#
#        sudo cp foo.crt /usr/share/ca-certificates/extra/foo.crt
#        Add the '.crt' file's path relative to /usr/share/ca-certificates to /etc/ca-certificates.conf
#
#        sudo dpkg-reconfigure ca-certificates
#        Update the installed CA's
#
#        sudo update-ca-certificates



CERT_TOOL=""
if [ "i686" = "$BUILDARCH" ]; then
    CERT_TOOL="./certutil_x86"
else
    CERT_TOOL="./certutil_x64"
fi


CERT_FILE="certificate.crt"
if [ -e "$1" ]; then
    CERT_FILE=$1
fi

CERT_NAME="Dorkbox LLC CA" 

# This installs for firefox
if [ -e ~/.mozilla* ]; then
	for CERT_DB in $(find  ~/.mozilla* -name "cert8.db")
    do
        CERT_DIR=$(dirname ${CERT_DB});
        #log "mozilla certificate" "install '${certificateName}' in ${CERT_DIR}"
        "${CERT_TOOL}" -d ${CERT_DIR} -A -n "${CERT_NAME}" -t "TCu,Cu,Cuw,Tuw" -i ${CERT_FILE} 
    done
fi

# This installs for thunderbird
if [ -e ~/.thunderbird* ]; then
	for CERT_DB in $(find  ~/.thunderbird* -name "cert8.db")
    do
        CERT_DIR=$(dirname ${CERT_DB});
        #log "mozilla certificate" "install '${certificateName}' in ${CERT_DIR}"
        "${CERT_TOOL}" -d ${CERT_DIR} -A -n "${CERT_NAME}" -t "TCu,Cu,Cuw,Tuw" -i ${CERT_FILE} 
    done
fi


# This installs for chrome
#NSS_DEFAULT_DB_TYPE="sql"
if [ ! -e ~/.pki/nssdb ]; then
	echo "==========================="
	echo "No Database found. Creating one..."
	echo "==========================="
    "${CERT_TOOL}" -N -d sql:$HOME/.pki/nssdb
fi
"${CERT_TOOL}" -d sql:$HOME/.pki/nssdb -A -n "${CERT_NAME}" -t "TCu,Cu,Cuw,Tuw" -i ${CERT_FILE} 
"${CERT_TOOL}" -d ~/.pki/nssdb -L

