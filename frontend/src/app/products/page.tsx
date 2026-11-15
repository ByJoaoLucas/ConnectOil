'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productsApi } from '@/lib/api';
import { Product } from '@/types';
import toast from 'react-hot-toast';
import {
  Plus,
  Search,
  Filter,
  Package,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Edit,
  Trash2,
  ArrowUp,
  ArrowDown,
} from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

interface ProductFormData {
  name: string;
  description?: string;
  category: string;
  unit: string;
  pricePerUnit: number;
  stockQuantity: number;
  minStockQuantity: number;
  isActive: boolean;
  imageUrl?: string;
}

const productSchema = z.object({
  name: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres').max(100),
  description: z.string().max(500).optional(),
  category: z.string().min(1, 'Categoria é obrigatória'),
  unit: z.string().min(1, 'Unidade é obrigatória'),
  pricePerUnit: z.number().min(0.01, 'Preço deve ser maior que zero'),
  stockQuantity: z.number().min(0, 'Estoque não pode ser negativo').default(0),
  minStockQuantity: z.number().min(0, 'Estoque mínimo não pode ser negativo').default(0),
  isActive: z.boolean().default(true),
  imageUrl: z.string().url('URL inválida').optional().or(z.literal('')),
});

const CATEGORIES = ['Limpeza', 'Cozinha', 'Higiene', 'Outros'];
const UNITS = ['un', 'kg', 'L', 'cx', 'pct'];

export default function ProductsPage() {
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [stockFilter, setStockFilter] = useState<'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'>('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const queryClient = useQueryClient();

  const { data: productsData, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => productsApi.getAllList(),
  });

  const products = productsData?.data || [];

  const filteredProducts = products.filter((product) => {
    const matchesSearch =
      product.name.toLowerCase().includes(search.toLowerCase()) ||
      product.category.toLowerCase().includes(search.toLowerCase()) ||
      product.description?.toLowerCase().includes(search.toLowerCase());
    const matchesCategory = !categoryFilter || product.category === categoryFilter;
    let matchesStock = true;
    if (stockFilter === 'IN_STOCK') matchesStock = product.isInStock && !product.isLowStock;
    else if (stockFilter === 'LOW_STOCK') matchesStock = product.isLowStock;
    else if (stockFilter === 'OUT_OF_STOCK') matchesStock = !product.isInStock;
    return matchesSearch && matchesCategory && matchesStock;
  });

  const categories = [...new Set(products.map(p => p.category))].sort();

  const createMutation = useMutation({
    mutationFn: (data: ProductFormData) => productsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Produto criado com sucesso!');
      setIsModalOpen(false);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao criar produto');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProductFormData }) =>
      productsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Produto atualizado com sucesso!');
      setIsModalOpen(false);
      setEditingProduct(null);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar produto');
    },
  });

  const updateStockMutation = useMutation({
    mutationFn: ({ id, quantityChange }: { id: string; quantityChange: number }) =>
      productsApi.updateStock(id, quantityChange),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Estoque atualizado com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao atualizar estoque');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => productsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Produto excluído com sucesso!');
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Erro ao excluir produto');
    },
  });

  const handleSubmit = (data: ProductFormData) => {
    const cleanData = { ...data };
    if (!cleanData.imageUrl) delete cleanData.imageUrl;
    if (editingProduct) {
      updateMutation.mutate({ id: editingProduct.id, data: cleanData });
    } else {
      createMutation.mutate(cleanData);
    }
  };

  const handleEdit = (product: Product) => {
    setEditingProduct(product);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Tem certeza que deseja excluir este produto?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleStockAdjust = (product: Product, change: number) => {
    if (product.stockQuantity + change < 0) {
      toast.error('Estoque não pode ficar negativo');
      return;
    }
    updateStockMutation.mutate({ id: product.id, quantityChange: change });
  };

  const openCreateModal = () => {
    setEditingProduct(null);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Produtos para Troca</h1>
          <p className="page-subtitle">Gerencie os produtos disponíveis para troca por óleo coletado</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="w-4 h-4 mr-2" />
          Novo Produto
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por nome, categoria, descrição..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="input pl-10"
              />
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="">Todas as categorias</option>
                {categories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={stockFilter}
                onChange={(e) => setStockFilter(e.target.value as typeof stockFilter)}
                className="input pl-10 pr-10 appearance-none"
              >
                <option value="ALL">Todos os estoques</option>
                <option value="IN_STOCK">Em estoque</option>
                <option value="LOW_STOCK">Estoque baixo</option>
                <option value="OUT_OF_STOCK">Sem estoque</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="spinner w-8 h-8"></div>
            </div>
          ) : (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Produto</th>
                    <th>Categoria</th>
                    <th>Unidade</th>
                    <th>Preço/Unidade</th>
                    <th>Estoque Atual</th>
                    <th>Estoque Mín.</th>
                    <th>Status</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredProducts.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-12 text-gray-500">
                        <Package className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                        <p className="text-lg">Nenhum produto encontrado</p>
                        <p className="text-sm">Tente ajustar os filtros ou cadastre um novo produto</p>
                      </td>
                    </tr>
                  ) : (
                    filteredProducts.map((product) => (
                      <tr key={product.id}>
                        <td>
                          <div>
                            <p className="font-medium text-gray-900">{product.name}</p>
                            {product.description && (
                              <p className="text-sm text-gray-500 truncate max-w-xs">{product.description}</p>
                            )}
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{product.category}</td>
                        <td className="text-sm text-gray-600">{product.unit}</td>
                        <td className="font-medium text-gray-900">
                          R$ {product.pricePerUnit.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </td>
                        <td>
                          <div className="flex items-center space-x-2">
                            <span className={`text-sm font-medium ${product.isInStock ? 'text-gray-900' : 'text-red-600'}`}>
                              {product.stockQuantity.toLocaleString('pt-BR')} {product.unit}
                            </span>
                            <div className="flex items-center space-x-1">
                              <button
                                onClick={() => handleStockAdjust(product, -1)}
                                disabled={updateStockMutation.isPending || product.stockQuantity <= 0}
                                className="p-1 rounded text-gray-500 hover:text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                                title="Remover 1"
                              >
                                <ArrowDown className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleStockAdjust(product, 1)}
                                disabled={updateStockMutation.isPending}
                                className="p-1 rounded text-gray-500 hover:text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                                title="Adicionar 1"
                              >
                                <ArrowUp className="w-4 h-4" />
                              </button>
                            </div>
                          </div>
                        </td>
                        <td className="text-sm text-gray-600">{product.minStockQuantity.toLocaleString('pt-BR')} {product.unit}</td>
                        <td>
                          <span className={`badge ${
                            !product.isInStock ? 'badge-danger' :
                            product.isLowStock ? 'badge-warning' :
                            product.isActive ? 'badge-success' : 'badge-gray'
                          }`}>
                            {!product.isInStock ? 'Sem estoque' :
                            product.isLowStock ? 'Estoque baixo' :
                            product.isActive ? 'Disponível' : 'Inativo'}
                          </span>
                        </td>
                        <td className="text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              onClick={() => handleEdit(product)}
                              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                              title="Editar"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleDelete(product.id)}
                              className="p-2 rounded-lg text-gray-500 hover:text-red-600 hover:bg-red-50"
                              title="Excluir"
                            >
                              <Trash2 className="w-5 h-5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900">
                {editingProduct ? 'Editar Produto' : 'Novo Produto'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <ProductForm
              initialData={editingProduct}
              onSubmit={handleSubmit}
              onClose={() => {
                setIsModalOpen(false);
                setEditingProduct(null);
              }}
              isLoading={createMutation.isPending || updateMutation.isPending}
            />
          </div>
        </div>
      )}
    </div>
  );
}

function ProductForm({
  initialData,
  onSubmit,
  onClose,
  isLoading,
}: {
  initialData: Product | null;
  onSubmit: (data: ProductFormData) => void;
  onClose: () => void;
  isLoading: boolean;
}) {
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: '',
      description: '',
      category: 'Limpeza',
      unit: 'un',
      pricePerUnit: 0,
      stockQuantity: 0,
      minStockQuantity: 0,
      isActive: true,
      imageUrl: '',
    },
  });

  if (initialData) {
    setValue('name', initialData.name);
    setValue('description', initialData.description || '');
    setValue('category', initialData.category);
    setValue('unit', initialData.unit);
    setValue('pricePerUnit', initialData.pricePerUnit);
    setValue('stockQuantity', initialData.stockQuantity);
    setValue('minStockQuantity', initialData.minStockQuantity);
    setValue('isActive', initialData.isActive);
    setValue('imageUrl', initialData.imageUrl || '');
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="modal-body space-y-4 p-6 max-h-[70vh] overflow-y-auto">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="md:col-span-2">
          <label className="label">Nome do Produto *</label>
          <input {...register('name')} className={`input ${errors.name ? 'input-error' : ''}`} placeholder="Ex: Detergente Líquido 500ml" />
          {errors.name && <p className="error-message">{errors.name.message}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="label">Descrição</label>
          <textarea {...register('description')} className="input" rows={3} placeholder="Descrição do produto" />
        </div>

        <div>
          <label className="label">Categoria *</label>
          <select {...register('category')} className={`input ${errors.category ? 'input-error' : ''}`}>
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
          {errors.category && <p className="error-message">{errors.category.message}</p>}
        </div>

        <div>
          <label className="label">Unidade *</label>
          <select {...register('unit')} className={`input ${errors.unit ? 'input-error' : ''}`}>
            {UNITS.map((u) => (
              <option key={u} value={u}>{u}</option>
            ))}
          </select>
          {errors.unit && <p className="error-message">{errors.unit.message}</p>}
        </div>

        <div>
          <label className="label">Preço por Unidade (R$) *</label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            {...register('pricePerUnit', { valueAsNumber: true })}
            className={`input ${errors.pricePerUnit ? 'input-error' : ''}`}
            placeholder="Ex: 12.50"
          />
          {errors.pricePerUnit && <p className="error-message">{errors.pricePerUnit.message}</p>}
        </div>

        <div>
          <label className="label">Estoque Atual</label>
          <input
            type="number"
            min="0"
            {...register('stockQuantity', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 100"
          />
        </div>

        <div>
          <label className="label">Estoque Mínimo</label>
          <input
            type="number"
            min="0"
            {...register('minStockQuantity', { valueAsNumber: true })}
            className="input"
            placeholder="Ex: 10"
          />
        </div>

        <div>
          <label className="label">Status</label>
          <select {...register('isActive')} className="input">
            <option value="true">Ativo</option>
            <option value="false">Inativo</option>
          </select>
        </div>

        <div className="md:col-span-2">
          <label className="label">URL da Imagem (opcional)</label>
          <input {...register('imageUrl')} type="url" className="input" placeholder="https://exemplo.com/imagem.jpg" />
        </div>
      </div>

      <div className="modal-footer">
        <button type="button" onClick={onClose} className="btn-outline">
          Cancelar
        </button>
        <button type="submit" disabled={isLoading} className="btn-primary">
          {isLoading ? (
            <span className="flex items-center space-x-2">
              <div className="spinner w-4 h-4"></div>
              <span>Salvando...</span>
            </span>
          ) : (
            initialData ? 'Atualizar' : 'Criar'
          )}
        </button>
      </div>
    </form>
  );
}