/**
 * Alertas: detección de cambios en lugares guardados y avisos en la app (RF-07, ADR 0012).
 * El envío por FCM llega en un paso posterior.
 * Reacciona a cada nueva opinión, sin procesos programados.
 *
 * <p>Capas: {@code domain} (reglas, sin Spring), {@code application} (casos de uso)
 * e {@code infrastructure} (web, persistencia e integraciones).
 */
package com.lenterosa.api.alerts;
