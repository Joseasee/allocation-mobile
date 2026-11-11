package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Deixa a câmera e a localização sempre liberadas para o site dentro do WebView.
 *
 * Uso na MainActivity:
 *   1) criar e ligar no WebView:   chrome = new PermissoesSite(this, "host.do.site.com.br");
 *                                  webView.setWebChromeClient(chrome);
 *   2) pedir as permissões do app: PermissoesSite.pedirPermissoes(this);   (antes de carregar o site)
 *   3) repassar o resultado:       chrome.aoResponderPermissoes(requestCode, permissions, grantResults);
 *                                  (dentro de onRequestPermissionsResult)
 */
public class PermissoesSite extends WebChromeClient {

    public static final int CODIGO_PERMISSOES = 4001;

    private static final String[] PERMISSOES = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private final Activity activity;
    private final String hostDoSite;

    private PermissionRequest cameraPendente;
    private GeolocationPermissions.Callback geoPendente;
    private String geoOrigemPendente;

    /**
     * @param hostDoSite só o endereço do site, sem https:// e sem barra (ex.: "meusite.com.br").
     *                   Só esse site recebe câmera e localização. Vazio = qualquer site.
     */
    public PermissoesSite(Activity activity, String hostDoSite) {
        this.activity = activity;
        this.hostDoSite = hostDoSite;
    }

    // ---------------- Permissões do Android (caixinha "Permitir") ----------------

    public static void pedirPermissoes(Activity activity) {
        List<String> faltando = new ArrayList<>();
        for (String p : PERMISSOES) {
            if (activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                faltando.add(p);
            }
        }
        if (!faltando.isEmpty()) {
            activity.requestPermissions(faltando.toArray(new String[0]), CODIGO_PERMISSOES);
        } else {
            avisarSeLocalizacaoDesligada(activity);
        }
    }

    public void aoResponderPermissoes(int codigo, String[] permissoes, int[] resultados) {
        if (codigo != CODIGO_PERMISSOES) return;

        boolean camera = temPermissao(Manifest.permission.CAMERA);
        boolean local = temLocalizacao();

        if (cameraPendente != null) {
            if (camera) cameraPendente.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else cameraPendente.deny();
            cameraPendente = null;
        }
        if (geoPendente != null) {
            geoPendente.invoke(geoOrigemPendente, local, local);
            geoPendente = null;
            geoOrigemPendente = null;
        }

        if (camera && local) {
            avisarSeLocalizacaoDesligada(activity);
            return;
        }

        Toast.makeText(activity, "Libere Câmera e Localização nas permissões do app.", Toast.LENGTH_LONG).show();

        // Se a pessoa marcou "não perguntar de novo", a caixinha não aparece mais: leva para as configurações do app.
        boolean bloqueado =
                (!camera && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                || (!local && !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION));
        if (bloqueado) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.getPackageName(), null));
            activity.startActivity(i);
        }
    }

    // ---------------- Pedidos do site (câmera e localização dentro do WebView) ----------------

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean querCamera = false;
                for (String r : request.getResources()) {
                    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) querCamera = true;
                }
                if (!querCamera || !origemPermitida(request.getOrigin())) {
                    request.deny();
                    return;
                }
                if (temPermissao(Manifest.permission.CAMERA)) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                } else {
                    cameraPendente = request;
                    activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, CODIGO_PERMISSOES);
                }
            }
        });
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        if (!origemPermitida(Uri.parse(origin))) {
            callback.invoke(origin, false, false);
            return;
        }
        if (temLocalizacao()) {
            callback.invoke(origin, true, true);
        } else {
            geoOrigemPendente = origin;
            geoPendente = callback;
            activity.requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, CODIGO_PERMISSOES);
        }
    }

    // ---------------- Auxiliares ----------------

    private boolean temPermissao(String permissao) {
        return activity.checkSelfPermission(permissao) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean temLocalizacao() {
        return temPermissao(Manifest.permission.ACCESS_FINE_LOCATION)
                || temPermissao(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private boolean origemPermitida(Uri origem) {
        if (hostDoSite == null || hostDoSite.isEmpty()) return true;
        return origem != null && hostDoSite.equalsIgnoreCase(origem.getHost());
    }

    // Se o GPS do celular estiver desligado, avisa e abre a tela de localização
    private static void avisarSeLocalizacaoDesligada(Activity activity) {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        boolean ligada = false;
        try {
            ligada = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            // ignora
        }
        if (!ligada) {
            Toast.makeText(activity, "Ligue a localização (GPS) do celular para usar o site.", Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }
}