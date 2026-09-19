-- ─────────────────────────────────────────────────────────────
--  item.servico
--
--  Marca itens que não têm estoque físico (serviços). Para eles não se
--  informa quantidade nem valor de compra — só o valor de venda.
--
--  Rodar uma vez, antes de subir a versão que lê a coluna.
-- ─────────────────────────────────────────────────────────────


ALTER TABLE public.item
  ADD COLUMN IF NOT EXISTS servico BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.item.servico IS
  'true = serviço (sem estoque físico): não tem quantidade nem valor de compra.';
